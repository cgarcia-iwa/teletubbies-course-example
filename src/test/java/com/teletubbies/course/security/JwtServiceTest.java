package com.teletubbies.course.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.teletubbies.course.config.SecurityProperties;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

class JwtServiceTest {

  private static final String SECRET =
      Base64.getEncoder()
          .encodeToString(
              "test-only-secret-with-at-least-32-bytes!".getBytes(StandardCharsets.UTF_8));

  private static final UserDetails INSTRUCTOR =
      User.withUsername("laalaa@teletubbies.test").password("irrelevant").roles("ADMINISTRATOR").build();

  private final JwtService jwtService =
      new JwtService(
          new SecurityProperties(null, new SecurityProperties.Jwt(SECRET, Duration.ofMinutes(5))));

  @Test
  void generateToken_should_embed_email_as_subject() {
    // WHEN
    final String token = jwtService.generateToken(INSTRUCTOR);

    // THEN
    assertThat(jwtService.extractUsername(token)).isEqualTo("laalaa@teletubbies.test");
    assertThat(jwtService.isTokenValid(token, INSTRUCTOR)).isTrue();
  }

  @Test
  void extractUsername_should_reject_expired_token() {
    // GIVEN
    final JwtService expiredJwtService =
        new JwtService(
            new SecurityProperties(null, new SecurityProperties.Jwt(SECRET, Duration.ofSeconds(-1))));
    final String token = expiredJwtService.generateToken(INSTRUCTOR);

    // WHEN / THEN
    assertThatThrownBy(() -> jwtService.extractUsername(token))
        .isInstanceOf(ExpiredJwtException.class);
  }

  @Test
  void extractUsername_should_reject_tampered_token() {
    // GIVEN
    final String token = jwtService.generateToken(INSTRUCTOR);
    final String tampered = token.substring(0, token.length() - 2) + "xx";

    // WHEN / THEN
    assertThatThrownBy(() -> jwtService.extractUsername(tampered))
        .isInstanceOf(JwtException.class);
  }
}
