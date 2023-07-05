package com.marcofaccani.awss3.unit.service;

import com.marcofaccani.awss3.service.BucketServiceImpl;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketResponse;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class BucketServiceImplTest {

  private final String BUCKET_NAME = "dummyBucketName";

  @Mock
  private S3Client s3Client;

  @InjectMocks
  private BucketServiceImpl underTest;

  private HeadBucketRequest mockDoesObjectExistsToReturnTrue() {
    final var expectedHeadBucketRequest = HeadBucketRequest.builder().bucket(BUCKET_NAME).build();
    final var mockResponse = mock(HeadBucketResponse.class);
    when(s3Client.headBucket(expectedHeadBucketRequest)).thenReturn(mockResponse);
    return expectedHeadBucketRequest;
  }

  private HeadBucketRequest mockDoesObjectExistsToReturnFalse() {
    final var expectedHeadBucketRequest = HeadBucketRequest.builder().bucket(BUCKET_NAME).build();
    doThrow(NoSuchBucketException.class)
        .when(s3Client)
        .headBucket(expectedHeadBucketRequest);
    return expectedHeadBucketRequest;
  }

  @Nested
  class DoesBucketExistTest {

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldReturnTrueWhenBucketExists(CapturedOutput logCapturer) {
      final var expectedRequest = mockDoesObjectExistsToReturnTrue();

      final var exists = assertDoesNotThrow(() -> underTest.doesBucketExist(BUCKET_NAME));
      assertTrue(exists);
      verify(s3Client).headBucket(expectedRequest);

      final var expectedLog = String.format(BucketServiceImpl.MSG_BUCKET_EXISTS, BUCKET_NAME);
      assertThat(logCapturer).contains(expectedLog);
    }

    @Test
    void shouldReturnFalseWhenBucketDoesNotExist() {
      final var expectedHeadBucketRequest = mockDoesObjectExistsToReturnFalse();

      final var exists = assertDoesNotThrow(() -> underTest.doesBucketExist(BUCKET_NAME));

      assertFalse(exists);
      verify(s3Client).headBucket(expectedHeadBucketRequest);
    }

    @Test
    void shouldThrowExceptionWithCustomErrMsgWhenAppDoesNotHavePrivilegesToAccessTheBucket() {
      final var expectedHeadBucketRequest = HeadBucketRequest.builder().bucket(BUCKET_NAME).build();
      final var unauthorizedException = S3Exception.builder()
          .statusCode(HttpStatus.UNAUTHORIZED.value())
          .build();

      when(s3Client.headBucket(expectedHeadBucketRequest)).thenThrow(unauthorizedException);

      final var ex = assertThrows(RuntimeException.class, () -> underTest.doesBucketExist(BUCKET_NAME));
      final var expectedErrMsg = String.format(BucketServiceImpl.ERR_MSG_UNAUTHORIZED, BUCKET_NAME);
      ;
      assertEquals(expectedErrMsg, ex.getMessage());
      verify(s3Client).headBucket(expectedHeadBucketRequest);
    }

    @Test
    void shouldPropagateExceptionWhenS3ExceptionIsThrownAndHttpStatusIsNot403() {
      final var expectedRequest = HeadBucketRequest.builder().bucket(BUCKET_NAME).build();

      final var errMsg = "error message";
      final var badRequestException = S3Exception.builder()
          .statusCode(HttpStatus.BAD_REQUEST.value())
          .message(errMsg)
          .build();

      when(s3Client.headBucket(expectedRequest)).thenThrow(badRequestException);

      final var ex = assertThrows(RuntimeException.class, () -> underTest.doesBucketExist(BUCKET_NAME));
      assertEquals(errMsg, ex.getMessage());
      verify(s3Client).headBucket(expectedRequest);
    }

  }

  @Nested
  class CreateBucketTest {

    @Test
    void shouldNotThrowExceptionWhenBucketAlreadyExists() {
      final var expectedHeadBucketRequest = mockDoesObjectExistsToReturnTrue();

      assertDoesNotThrow(() -> underTest.createBucket(BUCKET_NAME));
      verify(s3Client).headBucket(expectedHeadBucketRequest);

      final var createBucketRequest = CreateBucketRequest.builder().bucket(BUCKET_NAME).build();
      verify(s3Client, never()).createBucket(createBucketRequest);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldCreateBucketWhenItDoesNotExist(CapturedOutput logCapturer) {
      mockDoesObjectExistsToReturnFalse();
      assertDoesNotThrow(() -> underTest.createBucket(BUCKET_NAME));

      final var expectedS3Request = CreateBucketRequest.builder().bucket(BUCKET_NAME).build();
      verify(s3Client).createBucket(expectedS3Request);

      final var expectedLogMsg = String.format(BucketServiceImpl.MSG_BUCKET_CREATED, BUCKET_NAME);
      assertThat(logCapturer).contains(expectedLogMsg);
    }

    @Test
    void shouldThrowExceptionWithCustomErrMsgWhenS3ClientThrowsAnException() {
      mockDoesObjectExistsToReturnFalse();

      final var expectedS3Request = CreateBucketRequest.builder().bucket(BUCKET_NAME).build();
      final var s3ErrMsg = "s3 error message";
      when(s3Client.createBucket(expectedS3Request)).thenThrow(new RuntimeException(s3ErrMsg));

      final var ex = assertThrows(RuntimeException.class, () -> underTest.createBucket(BUCKET_NAME));
      final var expectedErrMsg = String.format(BucketServiceImpl.ERR_MSG_BUCKET_CREATION, BUCKET_NAME, s3ErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());
    }
  }

  @Nested
  class DeleteBucketTest {

    @Test
    void shouldNotThrowExceptionWhenBucketDoesNotExist() {
      final var expectedHeadBucketRequest = mockDoesObjectExistsToReturnFalse();

      assertDoesNotThrow(() -> underTest.deleteBucket(BUCKET_NAME));
      verify(s3Client).headBucket(expectedHeadBucketRequest);

      final var deleteBucketRequest = DeleteBucketRequest.builder().bucket(BUCKET_NAME).build();
      verify(s3Client, never()).deleteBucket(deleteBucketRequest);
    }

    @Test
    void shouldDeleteBucketWhenItExists() {
      final var expectedHeadBucketRequest = mockDoesObjectExistsToReturnTrue();

      final var deleteBucketRequest = DeleteBucketRequest.builder().bucket(BUCKET_NAME).build();
      assertDoesNotThrow(() -> underTest.deleteBucket(BUCKET_NAME));
      verify(s3Client).headBucket(expectedHeadBucketRequest);

      verify(s3Client).deleteBucket(deleteBucketRequest);
    }
  }

}