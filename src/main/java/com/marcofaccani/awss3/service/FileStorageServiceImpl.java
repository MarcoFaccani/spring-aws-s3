package com.marcofaccani.awss3.service;

import java.time.Duration;
import java.util.List;

import com.marcofaccani.awss3.config.AwsS3ConfigProperties;
import com.marcofaccani.awss3.exceptions.S3GetObjectException;
import com.marcofaccani.awss3.exceptions.S3ListObjectsException;
import com.marcofaccani.awss3.exceptions.S3PresignedUrlException;
import com.marcofaccani.awss3.exceptions.S3ObjectDeleteException;
import com.marcofaccani.awss3.exceptions.S3PutObjectException;
import com.marcofaccani.awss3.service.interfaces.BucketService;
import com.marcofaccani.awss3.service.interfaces.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.model.S3Object;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;

@Log4j2
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl implements FileStorageService {

  public static final String ERR_MSG_UPLOAD_FAILED = "Error while uploading file %s to AWS S3. Exception message: %s";
  public static final String ERR_MSG_DELETE_FAILED = "Error while deleting file %s from AWS S3. Exception message: %s";
  public static final String ERR_MSG_RETRIEVE_FAILED = "Error while retrieving file %s from AWS S3. Exception message: %s";
  public static final String ERR_MSG_LIST_BUCKET_CONTENT_FAILED = "Error while listing bucket %s content from AWS S3. Exception message: %s";
  public static final String ERR_MSG_GENERATE_PRESIGNEDURL_FAILED = "Error while generating pre-signed url for file with name %s. Exception message: %s";

  private final AwsS3ConfigProperties awsS3ConfigProperties;
  private final BucketService bucketService;
  private final S3Client s3Client;
  private final S3Presigner s3Presigner;


  @PostConstruct
  public void postConstruct() {
    createBucketIfNotExists(awsS3ConfigProperties.getBucketName());
  }

  private void createBucketIfNotExists(final String bucketName) {
    if (!bucketService.doesBucketExist(bucketName)) {
      bucketService.createBucket(bucketName);
    }
  }


  @Override
  public List<String> listFilesInBucket() {
    final var listRequest = ListObjectsV2Request.builder()
        .bucket(awsS3ConfigProperties.getBucketName())
        .build();

    try {
      final var listResponse = s3Client.listObjectsV2(listRequest);
      return listResponse.contents().stream().map(S3Object::key).toList();
    } catch (Exception ex) {
      final var errMsg = String.format(ERR_MSG_LIST_BUCKET_CONTENT_FAILED, awsS3ConfigProperties.getBucketName(),
          ex.getMessage());
      throw new S3ListObjectsException(errMsg);
    }
  }

  @Override
  public void uploadFile(final MultipartFile file) {
    final var request = PutObjectRequest.builder()
        .bucket(awsS3ConfigProperties.getBucketName())
        .key(file.getOriginalFilename())
        .build();

    try {
      s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
    } catch (Exception ex) {
      throw new S3PutObjectException(String.format(ERR_MSG_UPLOAD_FAILED, file.getOriginalFilename(), ex.getMessage()));
    }
  }

  @Override
  public void deleteFile(final String fileName) {
    final var request = DeleteObjectRequest.builder()
        .bucket(awsS3ConfigProperties.getBucketName())
        .key(fileName)
        .build();

    try {
      s3Client.deleteObject(request);
    } catch (Exception ex) {
      throw new S3ObjectDeleteException(String.format(ERR_MSG_DELETE_FAILED, fileName, ex.getMessage()));
    }
  }

  @Override
  public ResponseInputStream<GetObjectResponse> getFile(final String fileName) {
    final var request = GetObjectRequest.builder()
        .bucket(awsS3ConfigProperties.getBucketName())
        .key(fileName)
        .build();

    try {
      return s3Client.getObject(request);
    } catch (NoSuchKeyException ex) {
      throw ex;
    } catch (Exception ex) {
      throw new S3GetObjectException(String.format(ERR_MSG_RETRIEVE_FAILED, fileName, ex.getMessage()));
    }
  }

  @Override
  public String generatePreSignedUrlOfFile(final String fileName, final long expirationTimeInMinutes) {
    final var getObjectPresignRequest = GetObjectPresignRequest.builder()
        .signatureDuration(Duration.ofMinutes(expirationTimeInMinutes))
        .getObjectRequest(
            GetObjectRequest.builder()
                .bucket(awsS3ConfigProperties.getBucketName())
                .key(fileName)
                .build())
        .build();

    try {
      final var presignedGetObjectResponse = s3Presigner.presignGetObject(getObjectPresignRequest);
      return presignedGetObjectResponse.url().toString();
    } catch (Exception ex) {
      final var errMsg = String.format(ERR_MSG_GENERATE_PRESIGNEDURL_FAILED, fileName, ex.getMessage());
      throw new S3PresignedUrlException(errMsg);
    }
  }

}
