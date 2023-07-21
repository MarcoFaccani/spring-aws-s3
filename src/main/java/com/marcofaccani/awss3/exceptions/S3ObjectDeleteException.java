package com.marcofaccani.awss3.exceptions;

public class S3ObjectDeleteException extends RuntimeException {

  public S3ObjectDeleteException(String message) {
    super(message);
  }

}
