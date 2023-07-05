package com.marcofaccani.awss3.controller;

import com.marcofaccani.awss3.service.interfaces.BucketService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Log4j2
@RestController
@RequestMapping("/storage/buckets")
@RequiredArgsConstructor
public class BucketController {

  private final BucketService bucketService;

  @GetMapping("{bucketName}")
  public ResponseEntity<Boolean> doesBucketExist(@PathVariable final String bucketName) {
    final var exists = bucketService.doesBucketExist(bucketName);
    return new ResponseEntity<>(exists, HttpStatus.OK);
  }

  @PostMapping("{bucketName}")
  public ResponseEntity<HttpStatus> createBucket(@PathVariable final String bucketName) {
    bucketService.createBucket(bucketName);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("{bucketName}")
  public ResponseEntity<HttpStatus> deleteBucket(@PathVariable final String bucketName) {
    bucketService.deleteBucket(bucketName);
    return ResponseEntity.ok().build();
  }

}
