package com.marcofaccani.awss3.unit.service;

import java.nio.charset.Charset;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import com.marcofaccani.awss3.config.AwsS3ConfigProperties;
import com.marcofaccani.awss3.service.FileStorageServiceImpl;
import com.marcofaccani.awss3.service.interfaces.BucketService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.http.SdkHttpMethod;
import software.amazon.awssdk.http.SdkHttpRequest;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Response;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(SpringExtension.class)
class FileStorageServiceImplTest {

  private final String BUCKET_NAME = "dummyBucketName";
  private final String FILE_NAME = "dummyFileName";

  @Mock
  private S3Client s3Client;

  @Mock
  private S3Presigner s3Presigner;

  @Mock
  private AwsS3ConfigProperties awsS3ConfigProperties;

  @Mock
  private BucketService bucketService;

  @InjectMocks
  private FileStorageServiceImpl underTest;

  @BeforeEach
  void setup() {
    when(awsS3ConfigProperties.getBucketName()).thenReturn(BUCKET_NAME);
  }


  @Nested
  class PostConstructTest {

    @Test
    void shouldCreateBucketWhenItDoesNotExist() {
      when(bucketService.doesBucketExist(BUCKET_NAME)).thenReturn(false);

      assertDoesNotThrow(() -> underTest.postConstruct());
      verify(bucketService).createBucket(BUCKET_NAME);
    }

    @Test
    void shouldNotCreateBucketWhenItDoesExists() {
      when(bucketService.doesBucketExist(BUCKET_NAME)).thenReturn(true);

      assertDoesNotThrow(() -> underTest.postConstruct());
      verify(bucketService, never()).createBucket(BUCKET_NAME);
    }

  }

  @Nested
  class UploadFileTest {

    @Test
    void shouldUploadFile() {
      final var multipartFile = new MockMultipartFile(
          "file",
          "filename.txt",
          "text/plain",
          new byte[]{});

      final var expectedS3Request = PutObjectRequest.builder()
          .bucket(awsS3ConfigProperties.getBucketName())
          .key(multipartFile.getOriginalFilename())
          .build();

      assertDoesNotThrow(() -> underTest.uploadFile(multipartFile));
      verify(s3Client).putObject(eq(expectedS3Request), any(RequestBody.class));
    }

    @Test
    void shouldPropagateExceptionWithCustomErrMsg() {
      final var multipartFile = new MockMultipartFile(
          "file",
          "filename.txt",
          "text/plain",
          new byte[]{});

      final var expectedS3Request = PutObjectRequest.builder()
          .bucket(awsS3ConfigProperties.getBucketName())
          .key(multipartFile.getOriginalFilename())
          .build();

      final var originalExceptionErrMsg = "dummy error message";
      when(s3Client.putObject(eq(expectedS3Request), any(RequestBody.class))).thenThrow(
          new RuntimeException(originalExceptionErrMsg));

      final var ex = assertThrows(RuntimeException.class, () -> underTest.uploadFile(multipartFile));
      final var expectedErrMsg = String.format(FileStorageServiceImpl.ERR_MSG_UPLOAD_FAILED,
          multipartFile.getOriginalFilename(), originalExceptionErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());
    }

  }

  @Nested
  class DeleteFileTest {

    @Test
    void shouldDeleteFile() {
      assertDoesNotThrow(() -> underTest.deleteFile(FILE_NAME));

      final var expectedS3Request = DeleteObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(FILE_NAME)
          .build();
      verify(s3Client).deleteObject(expectedS3Request);
    }

    @Test
    void shouldPropagateExceptionWithCustomErrMsg() {
      final var expectedS3Request = DeleteObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(FILE_NAME)
          .build();
      final var originalExceptionErrMsg = "dummy error message";
      when(s3Client.deleteObject(expectedS3Request)).thenThrow(new RuntimeException(originalExceptionErrMsg));

      final var ex = assertThrows(RuntimeException.class, () -> underTest.deleteFile(FILE_NAME));
      final var expectedErrMsg = String.format(FileStorageServiceImpl.ERR_MSG_DELETE_FAILED, FILE_NAME,
          originalExceptionErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());

      verify(s3Client).deleteObject(expectedS3Request);
    }

  }

  @Nested
  class GetFileTest {

    @Test
    void shouldGetFile() {
      final var mockInputStream = mock(ResponseInputStream.class);
      final var expectedS3Request = GetObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(FILE_NAME)
          .build();

      when(s3Client.getObject(expectedS3Request)).thenReturn(mockInputStream);

      final var actualInputStream = assertDoesNotThrow(() -> underTest.getFile(FILE_NAME));

      assertEquals(mockInputStream, actualInputStream);
      verify(s3Client).getObject(expectedS3Request);
    }

    @Test
    void shouldPropagateExceptionWithCustomErrMsg() {
      final var expectedS3Request = GetObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(FILE_NAME)
          .build();
      final var originalExceptionErrMsg = "dummy error message";
      when(s3Client.getObject(expectedS3Request)).thenThrow(new RuntimeException(originalExceptionErrMsg));

      final var ex = assertThrows(RuntimeException.class, () -> underTest.getFile(FILE_NAME));
      final var expectedErrMsg = String.format(FileStorageServiceImpl.ERR_MSG_RETRIEVE_FAILED, FILE_NAME,
          originalExceptionErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());

      verify(s3Client).getObject(expectedS3Request);
    }

  }

  @Nested
  class ListBucketFilesTest {

    @Test
    void shouldListBucketFiles() {
      final var expectedS3Request = ListObjectsV2Request.builder()
          .bucket(awsS3ConfigProperties.getBucketName())
          .build();

      final var expectedFilesNamesList = List.of("name1.txt, name2.txt, name3.txt");
      final var mockObjects = new ArrayList<S3Object>();
      expectedFilesNamesList.forEach(fileName -> mockObjects.add(S3Object.builder().key(fileName).build()));

      final var mockedResponse = ListObjectsV2Response.builder().contents(mockObjects).build();
      when(s3Client.listObjectsV2(expectedS3Request)).thenReturn(mockedResponse);

      final var actualFilesNamesList = assertDoesNotThrow(() -> underTest.listFilesInBucket());

      assertEquals(expectedFilesNamesList, actualFilesNamesList);
    }

    @Test
    void shouldPropagateExceptionWithCustomErrMsg() {
      final var expectedS3Request = ListObjectsV2Request.builder()
          .bucket(awsS3ConfigProperties.getBucketName())
          .build();

      final var originalExceptionErrMsg = "dummy error message";
      when(s3Client.listObjectsV2(expectedS3Request)).thenThrow(new RuntimeException(originalExceptionErrMsg));

      final var ex = assertThrows(RuntimeException.class, () -> underTest.listFilesInBucket());

      final var expectedErrMsg = String.format(FileStorageServiceImpl.ERR_MSG_LIST_BUCKET_CONTENT_FAILED, BUCKET_NAME,
          originalExceptionErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());

      verify(s3Client).listObjectsV2(expectedS3Request);
    }

  }

  @Nested
  class GeneratePreSignedUrlTest {

    @Test
    void shouldGeneratePreSignedUrlOfGivenFile() {
      final var expirationTimeInMinutes = 2L;

      final var expectedRequest = GetObjectPresignRequest.builder()
          .signatureDuration(Duration.ofMinutes(expirationTimeInMinutes))
          .getObjectRequest(
              GetObjectRequest.builder()
                  .bucket(awsS3ConfigProperties.getBucketName())
                  .key(FILE_NAME)
                  .build())
          .build();

      final var protocol = "https";
      final var host = "dummy-host.com";
      final var expectedPreSignedUrl = protocol + "://" + host + "/";
      final var mockedResponse = PresignedGetObjectRequest.builder()
          .expiration(Instant.now())
          .signedPayload(SdkBytes.fromString(expectedPreSignedUrl, Charset.defaultCharset()))
          .isBrowserExecutable(true)
          .signedHeaders(Map.of("dummySignedHeader", Collections.singletonList("dummyValue")))
          .httpRequest(SdkHttpRequest.builder()
              .protocol(protocol)
              .host(host)
              .method(SdkHttpMethod.GET)
              .port(443)
              .encodedPath("/")
              .build())
          .build();
      when(s3Presigner.presignGetObject(expectedRequest)).thenReturn(mockedResponse);

      final var actualPreSignedUrl = assertDoesNotThrow(
          () -> underTest.generatePreSignedUrlOfFile(FILE_NAME, expirationTimeInMinutes));
      assertEquals(actualPreSignedUrl, expectedPreSignedUrl);
    }

    @Test
    void shouldPropagateExceptionWithCustomErrMsg() {
      final var originalExceptionErrMsg = "dummy error message";

      when(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class)))
          .thenThrow(new RuntimeException(originalExceptionErrMsg));

      final var ex = assertThrows(RuntimeException.class,
          () -> underTest.generatePreSignedUrlOfFile(FILE_NAME, 2L));

      final var expectedErrMsg = String.format(FileStorageServiceImpl.ERR_MSG_GENERATE_PRESIGNEDURL_FAILED, FILE_NAME,
          originalExceptionErrMsg);
      assertEquals(expectedErrMsg, ex.getMessage());

      verify(s3Presigner).presignGetObject(any(GetObjectPresignRequest.class));
    }

  }

}