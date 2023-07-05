package com.marcofaccani.awss3.service.interfaces;

public interface BucketService {

  void createBucket(String bucketName);

  void deleteBucket(String bucketName);

  boolean doesBucketExist(String bucketName);

}
