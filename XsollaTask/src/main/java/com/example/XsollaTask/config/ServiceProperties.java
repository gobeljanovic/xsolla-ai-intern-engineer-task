package com.example.XsollaTask.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties(prefix = "app.service")
@Validated
public record ServiceProperties(
        @NotBlank String version
) {
}