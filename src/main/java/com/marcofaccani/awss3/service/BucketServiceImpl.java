package com.marcofaccani.awss3.service;

import com.marcofaccani.awss3.exceptions.S3BucketCreationException;
import com.marcofaccani.awss3.exceptions.S3UnauthorizedException;
import com.marcofaccani.awss3.service.interfaces.BucketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CreateBucketRequest;
import software.amazon.awssdk.services.s3.model.DeleteBucketRequest;
import software.amazon.awssdk.services.s3.model.HeadBucketRequest;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.S3Exception;

// Please note, usually I would create CUSTOM Exceptions rather than using RuntimeException but for the sake of speed
// I omitted it (and the development of it is pretty straight forward)
@Service
@Log4j2
@RequiredArgsConstructor
public class BucketServiceImpl implements BucketService {

  public static final String MSG_BUCKET_NOT_EXISTS = "Bucket %s does not exist";
  public static final String MSG_BUCKET_EXISTS = "Bucket %s already exists";
  public static final String MSG_BUCKET_CREATED = "Bucket %s successfully created";
  public static final String MSG_BUCKET_DELETED = "Bucket %s successfully deleted";
  public static final String ERR_MSG_BUCKET_CREATION = "Error while creating bucket %s. Error message: %s";
  public static final String ERR_MSG_UNAUTHORIZED = "Application not authorized to access bucket %s. Potentially, the bucket exists but is not within company's AWS account domain or AWS account lacks the permissions to access it";
  private final S3Client s3Client;

  public boolean doesBucketExist(final String bucketName) {
    final var request = HeadBucketRequest.builder().bucket(bucketName).build();
    try {
      s3Client.headBucket(request);
    } catch (NoSuchBucketException ex) {
      log.info(String.format(MSG_BUCKET_NOT_EXISTS, bucketName));
      return false;
    } catch (S3Exception ex) {
      if (ex.statusCode() == HttpStatus.UNAUTHORIZED.value()) {
        final var errMsg = String.format(ERR_MSG_UNAUTHORIZED, bucketName);
        throw new S3UnauthorizedException(errMsg);
      }
      throw ex;
    }
    log.info(String.format(MSG_BUCKET_EXISTS, bucketName));
    return true;
  }

  @Override
  public void createBucket(final String bucketName) {
    if (!doesBucketExist(bucketName)) {
      final var request = CreateBucketRequest.builder().bucket(bucketName).build();
      try {
        s3Client.createBucket(request);
        log.info(String.format(MSG_BUCKET_CREATED, bucketName));
      } catch (Exception ex) {
        final var errMsg = String.format(ERR_MSG_BUCKET_CREATION, bucketName, ex.getMessage());
        throw new S3BucketCreationException(errMsg);
      }
    }
  }

  @Override
  public void deleteBucket(String bucketName) {
    if (doesBucketExist(bucketName)) {
      final var request = DeleteBucketRequest.builder().bucket(bucketName).build();
      s3Client.deleteBucket(request);
      log.info(String.format(MSG_BUCKET_DELETED, bucketName));
    }
  }

}
