package com.marcofaccani.awss3.integration;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.stream.IntStream;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.MultipartBodyBuilder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadObjectRequest;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.testcontainers.containers.localstack.LocalStackContainer.Service.S3;

@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
@AutoConfigureWebTestClient
public class FileStorageControllerIT {

  private static final String BUCKET_NAME = "dummy-bucket-name";
  private static final String ORIGINAL_FILE_NAME = "dummyFileName.txt";
  private static final String ORIGINAL_FILE_CONTENT = "Hello, World!";

  @Container
  static LocalStackContainer localStack;

  static {
    final var dockerImageName = DockerImageName.parse("localstack/localstack:2.1");
    localStack = new LocalStackContainer(dockerImageName).withServices(S3);
    localStack.start();
  }

  @BeforeAll
  static void createBucket() throws IOException, InterruptedException {
    localStack.execInContainer("awslocal", "s3", "mb", "s3://" + BUCKET_NAME);
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
  void setup() {
    baseUrl = "http://localhost:" + appPort + "/storage/files";
    assertBucketExists();
  }

  @AfterEach
  void tearDown() {
    deleteAllFilesInBucket(BUCKET_NAME);
  }

  @Nested
  class UploadFileIT {

    @Test
    void shouldUploadFile() throws IOException {
      //when
      uploadFileToS3(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);

      // then
      fetchFileFromBucketAndAssertContentEquals(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);
    }

    @Test
    void shouldOverwriteFileContentWhenFileAlreadyExists() throws IOException {
      // given
      uploadFileToS3(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);
      fetchFileFromBucketAndAssertContentEquals(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);

      // when
      final var newFileContent = "new file content";
      uploadFileToS3(ORIGINAL_FILE_NAME, newFileContent);

      // then
      fetchFileFromBucketAndAssertContentEquals(ORIGINAL_FILE_NAME, newFileContent);
    }

  }

  @Nested
  class DeleteFileIT {

    @Test
    void shouldDeleteFileWhenItExists() {
      uploadFileToS3(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);

      // when
      webTestClient.delete()
          .uri(baseUrl + "/{fileName}", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isOk();

      //then
      assertThrows(NoSuchKeyException.class, () -> s3Client.headObject(HeadObjectRequest.builder()
          .bucket(BUCKET_NAME)
          .key(ORIGINAL_FILE_CONTENT)
          .build()));
    }

    @Test
    void shouldReturn200WhenFileDoesNotExistToEnsureIdempotence() {
      webTestClient.delete()
          .uri(baseUrl + "/{fileName}", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isOk();

      webTestClient.delete()
          .uri(baseUrl + "/{fileName}", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isOk();
    }
  }

  @Nested
  class GetFileIT {

    @Test
    void shouldGetFileWhenItExists() {
      // when
      uploadFileToS3(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);

      // then
      webTestClient.get()
          .uri(baseUrl + "/{fileName}", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isOk()
          .expectBody(String.class)
          .isEqualTo(ORIGINAL_FILE_CONTENT);
    }

    @Test
    void shouldReturn400WhenFileDoesNotExist() {
      webTestClient.get()
          .uri(baseUrl + "/{fileName}", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isBadRequest();
    }

  }

  @Nested
  class ListFilesInBucketIT {

    @Test
    void shouldListFileNamesInBucket() {
      final var fileNamesList = IntStream.rangeClosed(1, 3)
          .mapToObj(i -> ORIGINAL_FILE_NAME + i)
          .toList();

      fileNamesList.forEach(fileName -> uploadFileToS3(fileName, ORIGINAL_FILE_CONTENT));

      final var response = webTestClient.get()
          .uri(baseUrl)
          .exchange()
          .expectStatus().isOk()
          .expectBody(String.class)
          .returnResult();

      assertEquals("[\"dummyFileName.txt1\",\"dummyFileName.txt2\",\"dummyFileName.txt3\"]",
          response.getResponseBody());
    }

  }

  @Nested
  class ShareFileIT {


    @Test
    void shouldGeneratePreSignedUrlOfFile() {
      uploadFileToS3(ORIGINAL_FILE_NAME, ORIGINAL_FILE_CONTENT);

      final var response = webTestClient.get()
          .uri(baseUrl + "/{fileName}/share?expirationTimeInMinutes=2", ORIGINAL_FILE_NAME)
          .exchange()
          .expectStatus().isOk()
          .expectBody(String.class)
          .returnResult();

      final var preSignedUrl = response.getResponseBody();
      assertNotNull(preSignedUrl);
      assertFalse(preSignedUrl.isBlank());
    }
  }

  private void assertBucketExists() {
    final var headBucketRequest = HeadBucketRequest.builder().bucket(BUCKET_NAME).build();
    final var headBucketResponse = assertDoesNotThrow(() -> s3Client.headBucket(headBucketRequest));
    assertNotNull(headBucketResponse);
  }

  private MultipartBodyBuilder createMultipartBuilder(final String fileName, final String content) {
    final var newMultipartBuilder = new MultipartBodyBuilder();
    newMultipartBuilder
        .part("file", content)
        .header(HttpHeaders.CONTENT_DISPOSITION, "form-data; name=file; filename=".concat(fileName));
    return newMultipartBuilder;
  }

  private void uploadFileToS3(final String fileName, final String content) {
    webTestClient.post()
        .uri(baseUrl + "/upload")
        .contentType(MediaType.MULTIPART_FORM_DATA)
        .bodyValue(createMultipartBuilder(fileName, content).build())
        .exchange()
        .expectStatus().isOk();
  }

  private void fetchFileFromBucketAndAssertContentEquals(final String fileName, final String contentToCompare)
      throws IOException {
    final var s3Object = s3Client.getObject(GetObjectRequest.builder()
        .bucket(BUCKET_NAME)
        .key(fileName)
        .build());

    final var s3ObjectContent = s3Object.readAllBytes();
    assertEquals(contentToCompare, new String(s3ObjectContent, StandardCharsets.UTF_8));
  }

  private void deleteAllFilesInBucket(final String bucketName) {
    final var listRequest = ListObjectsV2Request.builder()
        .bucket(bucketName)
        .build();

    final var listResponse = s3Client.listObjectsV2(listRequest);

    listResponse.contents().forEach(s3Object -> {
      final var deleteRequest = DeleteObjectRequest.builder()
          .bucket(bucketName)
          .key(s3Object.key())
          .build();

      s3Client.deleteObject(deleteRequest);
    });
  }

}
