package com.ecommerce.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "storage.s3")
@Getter
@Setter
public class S3Properties {
    private String endpoint = "http://localhost:9000";
    private String region = "us-east-1";
    private String accessKey = "nubea_admin";
    private String secretKey = "nubea_secret";
    private String bucket = "nubea";
}
