package com.marcofaccani.awss3.exceptions;

public class S3UnauthorizedException extends RuntimeException {

  public S3UnauthorizedException(String message) {
    super(message);
  }

}
