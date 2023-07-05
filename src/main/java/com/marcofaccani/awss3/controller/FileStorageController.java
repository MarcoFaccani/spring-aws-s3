package com.marcofaccani.awss3.controller;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.marcofaccani.awss3.service.interfaces.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;


@Log4j2
@RestController
@RequestMapping("/storage/files")
@RequiredArgsConstructor
public class FileStorageController {

  private final FileStorageService fileStorageService;

  @GetMapping
  public ResponseEntity<List<String>> listFilesInBucket() {
    final var fileNamesList = fileStorageService.listFilesInBucket();
    return ResponseEntity.ok().body(fileNamesList);

  }

  @PostMapping("/upload")
  public ResponseEntity<HttpStatus> uploadFile(MultipartFile file) {
    fileStorageService.uploadFile(file);
    return ResponseEntity.ok().build();
  }

  @DeleteMapping("/{fileName}")
  public ResponseEntity<HttpStatus> deleteFile(@PathVariable String fileName) {
    fileStorageService.deleteFile(fileName);
    return ResponseEntity.ok().build();
  }

  @GetMapping("/{fileName}")
  public ResponseEntity<StreamingResponseBody> getFile(@PathVariable String fileName) {
    final var responseInputStream = fileStorageService.getFile(fileName);

    StreamingResponseBody body = outputStream -> {
      try (InputStream inputStream = responseInputStream) {
        int numberOfBytesToWrite;
        byte[] data = new byte[1024];
        while ((numberOfBytesToWrite = inputStream.read(data, 0, data.length)) != -1) {
          outputStream.write(data, 0, numberOfBytesToWrite);
        }
      } catch (IOException e) {
        log.error("Error while reading stream from getFile");
      }
    };

    return new ResponseEntity<>(body, HttpStatus.OK);
  }

  @GetMapping("/{fileName}/share")
  public ResponseEntity<String> getPreSignedUrlToFile(@PathVariable String fileName,
      @RequestParam long expirationTimeInMinutes) {
    final var preSignedUrlOfFile = fileStorageService.generatePreSignedUrlOfFile(fileName, expirationTimeInMinutes);
    return new ResponseEntity<>(preSignedUrlOfFile, HttpStatus.OK);
  }

}
