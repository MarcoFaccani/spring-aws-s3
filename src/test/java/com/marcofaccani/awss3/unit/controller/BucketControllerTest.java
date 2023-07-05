package com.marcofaccani.awss3.unit.controller;

import com.marcofaccani.awss3.controller.BucketController;
import com.marcofaccani.awss3.service.interfaces.BucketService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(SpringExtension.class)
@WebMvcTest(BucketController.class)
class BucketControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @MockBean
  private BucketService bucketService;

  private final String BASE_URL = "/storage/buckets";
  private final String BUCKET_NAME = "dummyBucketName";


  @Test
  void doesBucketExist() throws Exception {
    when(bucketService.doesBucketExist(BUCKET_NAME)).thenReturn(true);

    final var callResponse = mockMvc.perform(MockMvcRequestBuilders.get(BASE_URL + "/{bucketName}", BUCKET_NAME)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(bucketService).doesBucketExist(BUCKET_NAME);
    assertTrue(Boolean.parseBoolean(callResponse.getResponse().getContentAsString()));
  }

  @Test
  void createBucket() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.post(BASE_URL + "/{bucketName}", BUCKET_NAME)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(bucketService).createBucket(BUCKET_NAME);
  }

  @Test
  void deleteBucket() throws Exception {
    mockMvc.perform(MockMvcRequestBuilders.delete(BASE_URL + "/{bucketName}", BUCKET_NAME)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andReturn();

    verify(bucketService).deleteBucket(BUCKET_NAME);
  }
}