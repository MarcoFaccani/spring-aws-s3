package com.marcofaccani.awss3.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "app.aws.s3")
public class AwsS3ConfigProperties {

  private final String bucketName;

}
