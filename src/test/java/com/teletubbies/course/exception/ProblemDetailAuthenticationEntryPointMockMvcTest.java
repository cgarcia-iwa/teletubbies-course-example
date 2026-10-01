package com.teletubbies.course.exception;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.teletubbies.course.config.SecurityConfig;
import com.teletubbies.course.course.CourseController;
import com.teletubbies.course.course.CourseService;
import com.teletubbies.course.security.JwtService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultMatcher;

/**
 * Spring Security raises the 401 before reaching the controller, so the advice never sees it: this
 * covers the custom entry point, which produces the same shape as the rest of the errors.
 */
@WebMvcTest(CourseController.class)
@Import({SecurityConfig.class, ProblemDetailAuthenticationEntryPoint.class})
class ProblemDetailAuthenticationEntryPointMockMvcTest {

  private static final String TYPE_BASE_URL = "https://api.teletubbies.dev/problems/";
  private static final String TRACE_ID_REGEX =
      "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  @Autowired private MockMvc mockMvc;

  @MockitoBean private CourseService courseService;
  @MockitoBean private JwtService jwtService;
  @MockitoBean private UserDetailsService userDetailsService;

  @Test
  void should_return_problem_detail_when_request_is_not_authenticated() throws Exception {
    mockMvc
        .perform(get("/courses"))
        .andExpect(status().isUnauthorized())
        .andExpect(problemDetail(HttpStatus.UNAUTHORIZED, "unauthorized", "/courses"));
  }

  private ResultMatcher problemDetail(
      final HttpStatus status, final String type, final String instance) {
    return result -> {
      assertThat(result.getResponse().getContentType())
          .as("content type")
          .startsWith(MediaType.APPLICATION_PROBLEM_JSON_VALUE);

      // SUPPRESS_EXCEPTIONS: a missing property returns null instead of raising PathNotFound.
      final DocumentContext json =
          JsonPath.parse(
              result.getResponse().getContentAsString(),
              Configuration.builder().options(Option.SUPPRESS_EXCEPTIONS).build());

      assertThat(json.<String>read("type")).isEqualTo(TYPE_BASE_URL + type);
      assertThat(json.<Integer>read("status")).isEqualTo(status.value());
      assertThat(json.<String>read("title")).isEqualTo(status.getReasonPhrase());
      assertThat(json.<String>read("detail")).isNotBlank();
      assertThat(json.<String>read("instance")).isEqualTo(instance);
      assertThat(json.<String>read("traceId")).matches(TRACE_ID_REGEX);
      assertThat(json.<Object>read("errors")).as("errors must be omitted").isNull();
    };
  }
}
