package com.teletubbies.course.exception;


import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import static com.teletubbies.course.exception.GlobalExceptionHandler.*;

/**
 * Writes the 401 as an RFC 9457 {@link ProblemDetail}, with the same shape as the responses
 * produced by the {@link GlobalExceptionHandler}.
 *
 * <p>Spring Security raises the 401 before reaching the controller, so it cannot be handled by the
 * advice and is resolved here instead.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ProblemDetailAuthenticationEntryPoint implements AuthenticationEntryPoint {

  private final ObjectMapper objectMapper;

  @Override
  public void commence(
      final HttpServletRequest request,
      final HttpServletResponse response,
      final AuthenticationException authenticationException)
      throws IOException {

    final ProblemDetail problemDetail =
        ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED, DETAIL_UNAUTHORIZED);
    problemDetail.setType(URI.create(TYPE_UNAUTHORIZED));
    problemDetail.setInstance(URI.create(request.getRequestURI()));

    final String traceId = UUID.randomUUID().toString();
    problemDetail.setProperty(PROP_TRACE_ID, traceId);

    log.warn(
        "Unauthenticated request: status={} traceId={} path={} reason={}",
        HttpStatus.UNAUTHORIZED.value(),
        traceId,
        request.getRequestURI(),
        authenticationException.getMessage());

    response.setStatus(HttpStatus.UNAUTHORIZED.value());
    response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
    response.setCharacterEncoding("UTF-8");
    objectMapper.writeValue(response.getOutputStream(), problemDetail);
  }
}
