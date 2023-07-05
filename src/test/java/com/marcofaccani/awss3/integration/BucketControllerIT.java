package com.marcofaccani.awss3.integration;

import java.io.IOException;

import com.marcofaccani.awss3.service.BucketServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.Bucket;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class BucketControllerIT {

  private static final String BUCKET_NAME = "dummy-bucket-name";

  @Container
  static LocalStackContainer localStack;

  static {
    final var dockerImageName = DockerImageName.parse("localstack/localstack:2.1");
    localStack = new LocalStackContainer(dockerImageName).withServices(S3);
    localStack.start();
  }

  @DynamicPropertySource
  static void overrideConfiguration(DynamicPropertyRegistry registry) {
    registry.add("app.aws.s3.bucket-name", () -> BUCKET_NAME);
    registry.add("spring.cloud.aws.endpoint", () -> localStack.getEndpointOverride(S3));
    registry.add("spring.cloud.aws.region.static", localStack::getRegion);
    registry.add("spring.cloud.aws.credentials.access-key", localStack::getAccessKey);
    registry.add("spring.cloud.aws.credentials.secret-key", localStack::getSecretKey);
  }

  private String baseUrl;

  @LocalServerPort
  private int appPort;

  @Autowired
  private WebTestClient webTestClient;

  @Autowired
  private S3Client s3Client;


  @BeforeEach
  void setup() throws IOException, InterruptedException {
    baseUrl = "http://localhost:" + appPort + "/storage/buckets";
    localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
  }

  @AfterEach
  void tearDown() {
    deleteAllBucketsExceptDefaultOne();
  }

  @Nested
  class DoesBucketExistIT {

    @Test
    void shouldReturnTrueWhenBucketExists() {
      assertBucketExists(BUCKET_NAME);

      webTestClient.get()
          .uri(baseUrl + "/{bucketName}", BUCKET_NAME)
          .exchange()
          .expectStatus().isOk()
          .expectBody(Boolean.class)
          .isEqualTo(true);
    }

    @Test
    void shouldReturnFalseWhenBucketDoesNotExist() {
      final var nonExistingBucketName = "non-existing-bucket";
      assertBucketDoesNotExist(nonExistingBucketName);

      webTestClient.get()
          .uri(baseUrl + "/{bucketName}", nonExistingBucketName)
          .exchange()
          .expectStatus().isOk()
          .expectBody(Boolean.class)
          .isEqualTo(false);
    }
  }

  @Nested
  class CreateBucketIT {

    @Test
    void shouldCreteBucketWhenItDoesNotExist() {
      final var newBucketName = "new-bucket-name";
      assertBucketDoesNotExist(newBucketName);

      webTestClient.post()
          .uri(baseUrl + "/{bucketName}", newBucketName)
          .exchange()
          .expectStatus().isOk();

      assertBucketExists(newBucketName);
    }

    @Test
    @ExtendWith(OutputCaptureExtension.class)
    void shouldNotCreateBucketWhenItAlreadyExists(CapturedOutput logCapturer) {
      assertBucketExists(BUCKET_NAME);

      webTestClient.post()
          .uri(baseUrl + "/{bucketName}", BUCKET_NAME)
          .exchange()
          .expectStatus().isOk();

      final var expectedLogMsg = String.format(BucketServiceImpl.MSG_BUCKET_EXISTS, BUCKET_NAME);
      assertThat(logCapturer).contains(expectedLogMsg);
    }
  }

  @Nested
  class DeleteBucketIT {

    @Test
    void shouldDeleteBucketWhenItExists() {
      assertBucketExists(BUCKET_NAME);

      webTestClient.delete()
          .uri(baseUrl + "/{bucketName}", BUCKET_NAME)
          .exchange()
          .expectStatus().isOk();

      assertBucketDoesNotExist(BUCKET_NAME);
    }

    @Test
    void shouldNotThrowExceptionWhenAttemptingToDeleteABucketThatDoesNotExist() {
      final var nonExistingBucketName = "non-existing-bucket-name";
      assertBucketDoesNotExist(nonExistingBucketName);

      webTestClient.delete()
          .uri(baseUrl + "/{bucketName}", nonExistingBucketName)
          .exchange()
          .expectStatus().isOk();
    }
  }

  private void assertBucketExists(String bucketName) {
    final var headBucketRequest = HeadBucketRequest.builder().bucket(bucketName).build();
    final var headBucketResponse = assertDoesNotThrow(() -> s3Client.headBucket(headBucketRequest));
    assertNotNull(headBucketResponse);
  }

  private void assertBucketDoesNotExist(String bucketName) {
    final var request = HeadBucketRequest.builder().bucket(bucketName).build();
    assertThrows(NoSuchBucketException.class, () -> s3Client.headBucket(request));
  }

  private void deleteAllBucketsExceptDefaultOne() {
    try {
      final var listBucketsResponse = s3Client.listBuckets();

      // filter buckets to delete
      final var bucketsToDelete = listBucketsResponse.buckets().stream()
          .map(Bucket::name)
          .filter(bucketName -> !bucketName.equalsIgnoreCase(BUCKET_NAME))
          .toList();

      // delete the buckets
      bucketsToDelete.forEach(bucketName -> {
        try {
          s3Client.deleteBucket(builder -> builder.bucket(bucketName));
          System.out.println("Bucket deleted: " + bucketName);
        } catch (S3Exception e) {
          throw new RuntimeException(
              "Error cleaning up buckets between IT tests: Bucket that caused the error:" + bucketName + "; ErrMsg: "
                  + e.getMessage());
        }
      });
    } catch (S3Exception e) {
      throw new RuntimeException(
          "Error listing buckets during clean up of the buckets between IT tests; ErrMsg: " + e.getMessage());
    }
  }

}
