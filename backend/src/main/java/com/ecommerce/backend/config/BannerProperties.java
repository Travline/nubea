package com.ecommerce.backend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "banner")
@Getter
@Setter
public class BannerProperties {
    private String storagePrefix = "banners/";
    private long maxFileSize = 5242880L; // 5 MB
    private List<String> allowedMimeTypes = List.of("image/jpeg", "image/png", "image/webp", "image/gif");
}
