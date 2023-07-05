package com.marcofaccani.awss3;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan
@Log4j2
public class Application {

  public static void main(String[] args) {
    SpringApplication.run(Application.class, args);
  }

}
