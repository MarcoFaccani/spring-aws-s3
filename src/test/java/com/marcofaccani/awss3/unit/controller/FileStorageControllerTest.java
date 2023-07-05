package com.marcofaccani.awss3.unit.controller;

import com.marcofaccani.awss3.controller.FileStorageController;
import com.marcofaccani.awss3.service.interfaces.FileStorageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;


@ExtendWith(SpringExtension.class)
@WebMvcTest(FileStorageController.class)
class FileStorageControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private FileStorageService fileStorageService;

  private final String BASE_URL = "/storage/files";
  private final String FILE_NAME = "dummyFileName";

  @Test
  void shouldUploadFile() throws Exception {
    final var multipartFile = new MockMultipartFile("file", new byte[]{});

    mockMvc.perform(MockMvcRequestBuilders.multipart(BASE_URL + "/upload").file(multipartFile))
        .andExpect(status().isOk());
    verify(fileStorageService).uploadFile(multipartFile);
  }

  @Test
  void shouldDeleteFile() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/{fileName}", FILE_NAME)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(fileStorageService).deleteFile(FILE_NAME);
  }

  @Test
  void shouldGetFile() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{fileName}", FILE_NAME)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(fileStorageService).getFile(FILE_NAME);
  }

  @Test
  void shouldListBucketFiles() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(fileStorageService).listFilesInBucket();
  }

  @Test
  void shouldGeneratePreSignedUrl() throws Exception {
    final var expirationTimeInMinutes = 2L;
    mockMvc.perform(
            MockMvcRequestBuilders.get(BASE_URL + "/{fileName}/share?expirationTimeInMinutes=" + expirationTimeInMinutes,
                    FILE_NAME)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(fileStorageService).generatePreSignedUrlOfFile(FILE_NAME, expirationTimeInMinutes);
  }

}