package com.marcofaccani.awss3.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import software.amazon.awssdk.services.s3.model.NoSuchBucketException;
import software.amazon.awssdk.services.s3.model.NoSuchKeyException;

@ControllerAdvice
public class OutboundExceptionHandler {

  public static final String ERR_MSG_BUCKET_NOT_FOUND = "Bucket not found";

  @ExceptionHandler(NoSuchKeyException.class)
  public ResponseEntity<String> handleFileNotFound() {
    return ResponseEntity.status(HttpStatus.BAD_REQUEST)
        .body("File not found in S3 for given file name");
  }

  @ExceptionHandler(NoSuchBucketException.class)
  public ResponseEntity<String> handleBucketNotFound() {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
        .body(ERR_MSG_BUCKET_NOT_FOUND);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<String> handleUnkownError(Exception ex) {
    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ex.getMessage());
  }

}
