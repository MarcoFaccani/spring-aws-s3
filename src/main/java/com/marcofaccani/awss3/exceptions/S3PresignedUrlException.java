package com.marcofaccani.awss3.exceptions;

public class S3PresignedUrlException extends RuntimeException {

  public S3PresignedUrlException(String message) {
    super(message);
  }

}
