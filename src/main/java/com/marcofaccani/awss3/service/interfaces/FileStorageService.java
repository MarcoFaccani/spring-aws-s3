package com.marcofaccani.awss3.service.interfaces;

import java.util.List;

import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;

public interface FileStorageService {

  void uploadFile(MultipartFile file);

  void deleteFile(String fileName);

  ResponseInputStream<GetObjectResponse> getFile(String fileName);

  String generatePreSignedUrlOfFile(String fileName, long expirationTimeInMinutes);

  List<String> listFilesInBucket();
}
