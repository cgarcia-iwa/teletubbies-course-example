package com.teletubbies.course.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security")
public record SecurityProperties(Cors cors, Jwt jwt) {

  public record Cors(String allowOrigin) {}

  public record Jwt(String secret, Duration expiration) {}
}