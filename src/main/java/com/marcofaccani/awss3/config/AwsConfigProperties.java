package com.marcofaccani.awss3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "spring.cloud.aws")
public class AwsConfigProperties {

  private final AwsS3ConfigProperties awsS3ConfigProperties;

}
