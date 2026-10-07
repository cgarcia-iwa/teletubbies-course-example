package com.teletubbies.course.auth;

import com.teletubbies.course.AuthApi;
import com.teletubbies.course.model.LoginRequest;
import com.teletubbies.course.model.LoginResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AuthController implements AuthApi {

  private final AuthService authService;

  @Override
  public ResponseEntity<LoginResponse> login(final LoginRequest loginRequest) {
    return ResponseEntity.ok(authService.login(loginRequest));
  }
}