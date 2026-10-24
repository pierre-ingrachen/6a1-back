package com.takima.backskeleton.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.Arrays;

@Profile("prod")
@Component
public class ProductionCorsOriginsValidator {
    public ProductionCorsOriginsValidator(@Value("${app.cors.allowed-origins:}") String[] allowedOrigins) {
        boolean noUsableOrigin = Arrays.stream(allowedOrigins).allMatch(String::isEmpty);
        boolean wildcard = Arrays.stream(allowedOrigins).anyMatch("*"::equals);
        if (noUsableOrigin || wildcard) {
            throw new IllegalStateException("CORS_ALLOWED_ORIGINS must list explicit origins in production");
        }
    }
}
