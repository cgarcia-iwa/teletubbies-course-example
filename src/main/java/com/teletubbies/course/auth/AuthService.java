package com.teletubbies.course.auth;

import com.teletubbies.course.model.LoginRequest;
import com.teletubbies.course.model.LoginResponse;
import com.teletubbies.course.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

  private static final String TOKEN_TYPE = "Bearer";

  private final AuthenticationManager authenticationManager;
  private final JwtService jwtService;

  public LoginResponse login(final LoginRequest request) {
    final Authentication authentication;
    try {
      authentication =
          authenticationManager.authenticate(
              UsernamePasswordAuthenticationToken.unauthenticated(
                  request.getEmail(), request.getPassword()));
    } catch (final AuthenticationException e) {
      // Same message for "unknown email" and "wrong password" so we do not reveal which emails exist.
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    final UserDetails user = (UserDetails) authentication.getPrincipal();
    final String token = jwtService.generateToken(user);
    return LoginResponse.builder()
        .token(token)
        .tokenType(TOKEN_TYPE)
        .expiresIn(jwtService.getExpirationSeconds())
        .role(jwtService.extractRole(token))
        .build();
  }
}
