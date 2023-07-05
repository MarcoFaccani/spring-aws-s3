package com.marcofaccani.awss3.config;

import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Data
@Component
@RequiredArgsConstructor
public class AppProperties {

  private final AwsConfigProperties awsConfigProperties;

}
