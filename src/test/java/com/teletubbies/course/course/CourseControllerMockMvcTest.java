package com.teletubbies.course.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web layer (controller) tests for CourseController.
 *
 * <p>We use {@code @WebMvcTest} instead of a full integration test because we only care about:
 * routes, HTTP status codes, JSON serialization/deserialization, validation, and role-based
 * permissions. The real business logic lives in {@link CourseService}, so it is fully mocked
 * here — we don't care whether it "works correctly", only that the controller calls it properly
 * and translates its result (or its errors) into the correct HTTP/JSON shape.
 *
 * <p>Tests are grouped with {@code @Nested} by operation (create, list, get, update, delete),
 * plus a separate group for "cross-cutting" errors that aren't tied to a specific operation
 * (malformed JSON, unknown route, unsupported HTTP method, unexpected error).
 */
@WebMvcTest(CourseController.class)
class CourseControllerMockMvcTest {

  private static final String TYPE_BASE_URL = "https://api.teletubbies.dev/problems/";
  private static final String TRACE_ID_REGEX =
          "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

  private static final String COURSES_URL = "/courses";
  private static final String COURSE_URL = "/courses/{courseId}";

  @Autowired private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean // The service is mocked to keep the test focused on the web layer.
  private CourseService courseService;

  // ---------------------------------------------------------------------------------------------
  // Helpers for building test data (avoid repeating the same .builder()...build() blocks in
  // every test). Any nested test class can call these because non-static @Nested classes
  // inherit access to the outer class's members.
  // ---------------------------------------------------------------------------------------------

  private String generateRandomId() {
    return UUID.randomUUID().toString();
  }

  private InstructorResource buildInstructor(String id, String fullName, String email) {
    return InstructorResource.builder().id(id).fullName(fullName).email(email).build();
  }

  private InstructorResource buildTestInstructor(String id) {
    return buildInstructor(id, "Test Instructor", "test@example.com");
  }

  private CourseResource buildCourse(
          String id,
          String name,
          String description,
          CourseCategoryType category,
          int duration,
          CourseLevelType level,
          InstructorResource instructor) {
    return CourseResource.builder()
            .id(id)
            .name(name)
            .description(description)
            .category(category)
            .duration(duration)
            .level(level)
            .instructor(instructor)
            .build();
  }

  private NewCourseRequest.Builder buildNewCourseRequestBuilder(String instructorId) {
    return NewCourseRequest.builder()
            .name("Spring boot course")
            .description("IoC")
            .category(CourseCategoryType.PROGRAMMING)
            .duration(3)
            .level(CourseLevelType.BEGINNER)
            .instructorId(instructorId);
  }

  private UpdateCourseRequest.Builder buildUpdateCourseRequestBuilder(
          String instructorId) {
    return UpdateCourseRequest.builder()
            .name("Spring boot course updated")
            .description("IoC updated")
            .category(CourseCategoryType.PROGRAMMING)
            .duration(4)
            .level(CourseLevelType.INTERMEDIATE)
            .instructorId(instructorId);
  }

  // ---------------------------------------------------------------------------------------------
  // POST /courses
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("POST /courses — create a course")
  class WhenCreatingACourse {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("creates the course and returns 201 with the created course")
    void createCourse_should_return_201_with_the_created_course() {
      String instructorId = generateRandomId();
      NewCourseRequest request = buildNewCourseRequestBuilder(instructorId).build();

      InstructorResource instructor = buildTestInstructor(instructorId);
      CourseResource expected =
              buildCourse(
                      generateRandomId(),
                      request.getName(),
                      request.getDescription(),
                      request.getCategory(),
                      request.getDuration(),
                      request.getLevel(),
                      instructor);

      when(courseService.create(any(NewCourseRequest.class))).thenReturn(expected);

      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isCreated())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.course.id").value(expected.getId()))
              .andExpect(jsonPath("$.course.name").value("Spring boot course"))
              .andExpect(jsonPath("$.course.description").value("IoC"))
              .andExpect(jsonPath("$.course.duration").value(3))
              .andExpect(jsonPath("$.course.level").value(CourseLevelType.BEGINNER.toString()))
              .andExpect(
                      jsonPath("$.course.category").value(CourseCategoryType.PROGRAMMING.toString()))
              .andExpect(jsonPath("$.course.instructor.id").value(instructorId));

      verify(courseService).create(any(NewCourseRequest.class));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 409 when a course with that name already exists")
    void createCourse_should_return_409_when_name_already_exists() {
      NewCourseRequest request = buildNewCourseRequestBuilder(generateRandomId()).build();

      when(courseService.create(any(NewCourseRequest.class)))
              .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Course name already exists"));

      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isConflict())
              .andExpect(expectProblemDetail(HttpStatus.CONFLICT, "conflict", COURSES_URL))
              .andExpect(jsonPath("$.detail").value("Course name already exists"));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 404 when the given instructor does not exist")
    void createCourse_should_return_404_when_instructor_does_not_exist() {
      String instructorId = generateRandomId();
      NewCourseRequest request = buildNewCourseRequestBuilder(instructorId).build();

      when(courseService.create(any(NewCourseRequest.class)))
              .thenThrow(
                      new ResponseStatusException(
                              HttpStatus.NOT_FOUND, "Instructor not found: " + instructorId));

      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isNotFound())
              .andExpect(expectProblemDetail(HttpStatus.NOT_FOUND, "not-found", COURSES_URL))
              .andExpect(jsonPath("$.detail").value("Instructor not found: " + instructorId));

      verify(courseService).create(any(NewCourseRequest.class));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 400 with one error entry per violated validation rule")
    void createCourse_should_return_400_with_one_error_per_broken_rule() {
      // name and instructorId are left blank on purpose: each one breaks two rules
      // (must not be blank + size range), so we expect 4 errors total, not 2
      // (one per field).
      NewCourseRequest request =
              NewCourseRequest.builder()
                      .name("")
                      .category(CourseCategoryType.PROGRAMMING)
                      .duration(3)
                      .level(CourseLevelType.BEGINNER)
                      .instructorId("")
                      .build();

      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(expectValidationProblemDetail(HttpStatus.BAD_REQUEST, "bad-request", COURSES_URL))
              .andExpect(jsonPath("$.errors.length()").value(4))
              .andExpect(
                      jsonPath(
                              "$.errors[?(@.field == 'name')].message",
                              containsInAnyOrder("must not be blank", "size must be between 1 and 100")))
              .andExpect(
                      jsonPath(
                              "$.errors[?(@.field == 'instructorId')].message",
                              containsInAnyOrder("must not be blank", "size must be between 1 and 36")));

      verify(courseService, never()).create(any(NewCourseRequest.class));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 400 when the name exceeds the maximum length")
    void createCourse_should_return_400_when_name_is_too_long() {
      NewCourseRequest request =
              buildNewCourseRequestBuilder(generateRandomId())
                      .name("a".repeat(101)) // NewCourseRequest.name is limited to 100 characters
                      .build();

      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(expectValidationProblemDetail(HttpStatus.BAD_REQUEST, "bad-request", COURSES_URL))
              .andExpect(jsonPath("$.errors.length()").value(1))
              .andExpect(jsonPath("$.errors[0].field").value("name"))
              .andExpect(jsonPath("$.errors[0].message").value("size must be between 1 and 100"));

      verify(courseService, never()).create(any(NewCourseRequest.class));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 400 when the request body is not valid JSON")
    void createCourse_should_return_400_when_body_is_malformed() {
      mockMvc
              .perform(
                      post(COURSES_URL)
                              .with(csrf())
                              .content("{\"name\": ")
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(expectProblemDetail(HttpStatus.BAD_REQUEST, "bad-request", COURSES_URL));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // GET /courses
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /courses — list courses")
  class WhenListingCourses {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("returns 200 with every existing course")
    void getAllCourses_should_return_200_with_every_course() {
      InstructorResource instructor1 =
              buildInstructor(generateRandomId(), "Instructor One", "one@example.com");
      InstructorResource instructor2 =
              buildInstructor(generateRandomId(), "Instructor Two", "two@example.com");

      CourseResource first =
              buildCourse(
                      generateRandomId(),
                      "Spring boot course",
                      "IoC",
                      CourseCategoryType.PROGRAMMING,
                      3,
                      CourseLevelType.BEGINNER,
                      instructor1);

      CourseResource second =
              buildCourse(
                      generateRandomId(),
                      "Design basics",
                      "UI",
                      CourseCategoryType.DESIGN,
                      5,
                      CourseLevelType.INTERMEDIATE,
                      instructor2);

      when(courseService.getAll()).thenReturn(List.of(first, second));

      mockMvc
              .perform(get(COURSES_URL))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.courses.length()").value(2))
              .andExpect(jsonPath("$.courses[0].id").value(first.getId()))
              .andExpect(jsonPath("$.courses[0].name").value("Spring boot course"))
              .andExpect(jsonPath("$.courses[1].id").value(second.getId()))
              .andExpect(jsonPath("$.courses[1].name").value("Design basics"));

      verify(courseService).getAll();
    }
  }

  // ---------------------------------------------------------------------------------------------
  // GET /courses/{courseId}
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("GET /courses/{courseId} — get a single course")
  class WhenGettingACourse {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("returns 200 with the requested course")
    void getCourse_should_return_200_with_the_requested_course() {
      String courseId = generateRandomId();
      InstructorResource instructor = buildTestInstructor(generateRandomId());
      CourseResource expected =
              buildCourse(
                      courseId,
                      "Spring boot course",
                      "IoC",
                      CourseCategoryType.PROGRAMMING,
                      3,
                      CourseLevelType.BEGINNER,
                      instructor);

      when(courseService.getById(courseId)).thenReturn(expected);

      mockMvc
              .perform(get(COURSE_URL, courseId))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.course.id").value(courseId))
              .andExpect(jsonPath("$.course.name").value("Spring boot course"));

      verify(courseService).getById(courseId);
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("returns 404 when the course does not exist")
    void getCourse_should_return_404_when_course_does_not_exist() {
      String courseId = generateRandomId();

      when(courseService.getById(courseId))
              .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + courseId));

      mockMvc
              .perform(get(COURSE_URL, courseId))
              .andExpect(status().isNotFound())
              .andExpect(expectProblemDetail(HttpStatus.NOT_FOUND, "not-found", "/courses/" + courseId))
              .andExpect(jsonPath("$.detail").value("Course not found: " + courseId));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("returns 403 when the user is not allowed to access the course")
    void getCourse_should_return_403_when_access_is_denied() {
      String courseId = generateRandomId();

      when(courseService.getById(courseId))
              .thenThrow(new AccessDeniedException("Not the owner of the course"));

      mockMvc
              .perform(get(COURSE_URL, courseId))
              .andExpect(status().isForbidden())
              .andExpect(expectProblemDetail(HttpStatus.FORBIDDEN, "forbidden", "/courses/" + courseId));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // PUT /courses/{courseId}
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("PUT /courses/{courseId} — update a course")
  class WhenUpdatingACourse {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("updates the course and returns 200 with the new data")
    void updateCourse_should_return_200_with_the_new_data() {
      String courseId = generateRandomId();
      String instructorId = generateRandomId();
      InstructorResource instructor = buildTestInstructor(instructorId);

      UpdateCourseRequest request = buildUpdateCourseRequestBuilder(instructorId).build();

      CourseResource expected =
              buildCourse(
                      courseId,
                      request.getName(),
                      request.getDescription(),
                      request.getCategory(),
                      request.getDuration(),
                      request.getLevel(),
                      instructor);

      when(courseService.update(eq(courseId), any(UpdateCourseRequest.class))).thenReturn(expected);

      mockMvc
              .perform(
                      put(COURSE_URL, courseId)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isOk())
              .andExpect(content().contentType(MediaType.APPLICATION_JSON))
              .andExpect(jsonPath("$.course.id").value(courseId))
              .andExpect(jsonPath("$.course.name").value("Spring boot course updated"))
              .andExpect(jsonPath("$.course.duration").value(4))
              .andExpect(jsonPath("$.course.level").value(CourseLevelType.INTERMEDIATE.toString()))
              .andExpect(jsonPath("$.course.instructor.id").value(instructorId));

      verify(courseService).update(eq(courseId), any(UpdateCourseRequest.class));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 400 when instructorId is not a valid format")
    void updateCourse_should_return_400_when_instructor_id_is_invalid() {
      String courseId = generateRandomId();
      UpdateCourseRequest request = buildUpdateCourseRequestBuilder("not-a-uuid").build();

      when(courseService.update(eq(courseId), any(UpdateCourseRequest.class)))
              .thenThrow(
                      new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid instructorId: not-a-uuid"));

      mockMvc
              .perform(
                      put(COURSE_URL, courseId)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isBadRequest())
              .andExpect(expectProblemDetail(HttpStatus.BAD_REQUEST, "bad-request", "/courses/" + courseId))
              .andExpect(jsonPath("$.detail").value("Invalid instructorId: not-a-uuid"));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 404 when the given instructor does not exist")
    void updateCourse_should_return_404_when_instructor_does_not_exist() {
      String courseId = generateRandomId();
      String instructorId = generateRandomId();
      UpdateCourseRequest request = buildUpdateCourseRequestBuilder(instructorId).build();

      when(courseService.update(eq(courseId), any(UpdateCourseRequest.class)))
              .thenThrow(
                      new ResponseStatusException(
                              HttpStatus.NOT_FOUND, "Instructor not found: " + instructorId));

      mockMvc
              .perform(
                      put(COURSE_URL, courseId)
                              .with(csrf())
                              .content(objectMapper.writeValueAsString(request))
                              .contentType(MediaType.APPLICATION_JSON))
              .andExpect(status().isNotFound())
              .andExpect(expectProblemDetail(HttpStatus.NOT_FOUND, "not-found", "/courses/" + courseId))
              .andExpect(jsonPath("$.detail").value("Instructor not found: " + instructorId));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // DELETE /courses/{courseId}
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("DELETE /courses/{courseId} — delete a course")
  class WhenDeletingACourse {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("deletes the course and returns 204")
    void deleteCourse_should_return_204() {
      String courseId = generateRandomId();

      doNothing().when(courseService).delete(courseId);

      mockMvc
              .perform(delete(COURSE_URL, courseId).with(csrf()))
              .andExpect(status().isNoContent());

      verify(courseService).delete(courseId);
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 500 without leaking the internal error message on an unexpected failure")
    void deleteCourse_should_return_500_without_leaking_the_internal_message() {
      String courseId = generateRandomId();

      doThrow(new IllegalStateException("Connection to the database was lost"))
              .when(courseService)
              .delete(courseId);

      mockMvc
              .perform(delete(COURSE_URL, courseId).with(csrf()))
              .andExpect(status().isInternalServerError())
              .andExpect(
                      expectProblemDetail(
                              HttpStatus.INTERNAL_SERVER_ERROR, "internal-server-error", "/courses/" + courseId))
              // The "detail" exposed to the client is generic: the real cause only reaches the
              // server logs, it must never reach the response body.
              .andExpect(content().string(not(containsString("Connection to the database was lost"))));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Cross-cutting errors: not tied to a specific operation, but to the web framework itself
  // (unknown route, method not supported on a route that does exist).
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("General web framework errors")
  class WhenRequestDoesNotMatchAnyHandler {

    @SneakyThrows
    @Test
    @WithMockUser(roles = "TEACHER")
    @DisplayName("returns 404 when the route does not exist")
    void unmappedPath_should_return_404() {
      mockMvc
              .perform(get("/not-mapped"))
              .andExpect(status().isNotFound())
              .andExpect(expectProblemDetail(HttpStatus.NOT_FOUND, "not-found", "/not-mapped"));
    }

    @SneakyThrows
    @Test
    @WithMockUser(roles = "ADMINISTRATOR")
    @DisplayName("returns 405 when the HTTP method is not supported on that route")
    void patchCourse_should_return_405() {
      String courseId = generateRandomId();

      mockMvc
              .perform(patch(COURSE_URL, courseId).with(csrf()))
              .andExpect(status().isMethodNotAllowed())
              .andExpect(
                      expectProblemDetail(
                              HttpStatus.METHOD_NOT_ALLOWED, "method-not-allowed", "/courses/" + courseId));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Helpers to verify the ProblemDetail (RFC 7807) shape. Used from every @Nested class above.
  // ---------------------------------------------------------------------------------------------

  /**
   * Checks the shape shared by every ProblemDetail. The {@code errors} property only shows up on
   * validation failures, so it must be absent everywhere else.
   */
  private ResultMatcher expectProblemDetail(
          final HttpStatus status, final String type, final String instance) {
    return result -> {
      final DocumentContext json = assertProblem(result, status, type, instance);
      assertThat(json.<Object>read("errors")).as("errors must be omitted").isNull();
    };
  }

  /** Same as {@link #expectProblemDetail} but also requires the list of per-field violations. */
  private ResultMatcher expectValidationProblemDetail(
          final HttpStatus status, final String type, final String instance) {
    return result -> {
      final DocumentContext json = assertProblem(result, status, type, instance);
      assertThat(json.<Object>read("errors")).as("errors must be present").isNotNull();
    };
  }

  private DocumentContext assertProblem(
          final MvcResult result, final HttpStatus status, final String type, final String instance)
          throws Exception {
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

    return json;
  }
}