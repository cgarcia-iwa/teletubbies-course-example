package com.teletubbies.course.instructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;
import lombok.SneakyThrows;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.web.server.ResponseStatusException;

/**
 * Web-layer tests for {@link InstructorController}.
 *
 * <p>The service is mocked to isolate the web layer: these tests cover routing, JSON envelopes,
 * Bean Validation and the translation of exceptions into RFC 9457 problem details. Method security
 * is NOT active in this slice (see {@code InstructorControllerSecurityMockMvcTest}).
 */
@WebMvcTest(InstructorController.class)
class InstructorControllerMockMvcTest {

    private static final String TYPE_BASE_URL = "https://api.teletubbies.dev/problems/";
    private static final String TRACE_ID_REGEX =
            "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}";

    private static final String INSTRUCTORS_URL = "/instructors";
    private static final String INSTRUCTOR_URL = "/instructors/{instructorId}";

    private static final String FULL_NAME = "Laa-Laa";
    private static final String EMAIL = "laalaa@teletubbies.test";
    private static final String PASSWORD = "s3cret-pass";

    @Autowired private MockMvc mockMvc;

    // Mocked to isolate the web layer.
    @MockitoBean private InstructorService instructorService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    // ----------------------------------------------------------------------------------------------------
    // POST /instructors
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("POST /instructors — create an instructor")
    class WhenCreatingAnInstructor {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("201 returns the created instructor and never exposes the password")
        void createInstructor_should_return_201_without_exposing_password() {
            final String id = generateRandomId();
            final NewInstructorRequest request = buildNewInstructorRequestBuilder().build();
            when(instructorService.create(request))
                    .thenReturn(buildInstructor(id, FULL_NAME, EMAIL, InstructorRoleType.TEACHER));

            mockMvc
                    .perform(
                            post(INSTRUCTORS_URL)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.instructor.id").value(id))
                    .andExpect(jsonPath("$.instructor.fullName").value(FULL_NAME))
                    .andExpect(jsonPath("$.instructor.email").value(EMAIL))
                    .andExpect(jsonPath("$.instructor.role").value("TEACHER"))
                    // The hash must never travel back to the client.
                    .andExpect(jsonPath("$.instructor.password").doesNotExist());

            // The body reached the service intact: the stub above only matches an equal request.
            verify(instructorService).create(request);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("409 when the full name already exists")
        void createInstructor_should_return_409_when_full_name_already_exists() {
            final String detail = "Instructor full name already exists: " + FULL_NAME;
            when(instructorService.create(any(NewInstructorRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, detail));

            mockMvc
                    .perform(postCreate(buildNewInstructorRequestBuilder().build()))
                    .andExpect(status().isConflict())
                    .andExpect(
                            expectProblemDetail(HttpStatus.CONFLICT, TYPE_BASE_URL + "conflict", INSTRUCTORS_URL))
                    .andExpect(jsonPath("$.detail").value(detail));
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("409 when the email already exists")
        void createInstructor_should_return_409_when_email_already_exists() {
            final String detail = "Instructor email already exists: " + EMAIL;
            when(instructorService.create(any(NewInstructorRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, detail));

            mockMvc
                    .perform(postCreate(buildNewInstructorRequestBuilder().build()))
                    .andExpect(status().isConflict())
                    .andExpect(
                            expectProblemDetail(HttpStatus.CONFLICT, TYPE_BASE_URL + "conflict", INSTRUCTORS_URL))
                    .andExpect(jsonPath("$.detail").value(detail));
        }

        // Two concurrent creations can both pass the existsBy... checks and then collide on the
        // unique constraint: that path is handled by a different @ExceptionHandler.
        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("409 when the database rejects a duplicate, without leaking internals")
        void createInstructor_should_return_409_when_database_rejects_duplicate() {
            when(instructorService.create(any(NewInstructorRequest.class)))
                    .thenThrow(new DataIntegrityViolationException("duplicate key: instructor_full_name_uk"));

            mockMvc
                    .perform(postCreate(buildNewInstructorRequestBuilder().build()))
                    .andExpect(status().isConflict())
                    .andExpect(
                            expectProblemDetail(HttpStatus.CONFLICT, TYPE_BASE_URL + "conflict", INSTRUCTORS_URL))
                    .andExpect(
                            jsonPath("$.detail")
                                    .value("The request conflicts with the current state of the resource"))
                    .andExpect(content().string(not(containsString("instructor_full_name_uk"))));
        }

        // fullName and email are annotated twice (@NotBlank on the field, @NotNull/@Size on the
        // getter), so an empty value breaks two rules per field.
        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 one error per violated rule when fullName and email are empty")
        void createInstructor_should_return_400_with_one_error_per_violated_rule() {
            final NewInstructorRequest request =
                    buildNewInstructorRequestBuilder().fullName("").email("").build();

            mockMvc
                    .perform(postCreate(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectValidationProblemDetail(
                                    INSTRUCTORS_URL))
                    .andExpect(jsonPath("$.detail").value("The request contains invalid or missing fields"))
                    .andExpect(jsonPath("$.errors", hasSize(4)))
                    .andExpect(
                            jsonPath(
                                    "$.errors[*].message",
                                    containsInAnyOrder(
                                            "must not be blank",
                                            "must not be blank",
                                            "size must be between 1 and 150",
                                            "size must be between 1 and 150")));

            // Validation fails before the controller body runs: the service is never reached.
            verifyNoInteractions(instructorService);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 when required fields are missing from the body")
        void createInstructor_should_return_400_when_required_fields_are_missing() {
            mockMvc
                    .perform(
                            post(INSTRUCTORS_URL)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{}"))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectValidationProblemDetail(
                                    INSTRUCTORS_URL))
                    .andExpect(jsonPath("$.errors", hasSize(4)))
                    .andExpect(
                            jsonPath(
                                    "$.errors[*].message",
                                    containsInAnyOrder(
                                            "must not be blank",
                                            "must not be blank",
                                            "must not be null",
                                            "must not be null")));

            verifyNoInteractions(instructorService);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 a single error when the password is shorter than 7 characters")
        void createInstructor_should_return_400_when_password_is_too_short() {
            final NewInstructorRequest request =
                    buildNewInstructorRequestBuilder().password("abc").build();

            mockMvc
                    .perform(postCreate(request))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectValidationProblemDetail(
                                    INSTRUCTORS_URL))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("password"))
                    .andExpect(jsonPath("$.errors[0].message").value("size must be between 7 and 12"));

            verifyNoInteractions(instructorService);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 when the body is not valid JSON")
        void createInstructor_should_return_400_when_body_is_malformed() {
            mockMvc
                    .perform(
                            post(INSTRUCTORS_URL)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content("{ \"fullName\": "))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.BAD_REQUEST, TYPE_BASE_URL + "bad-request", INSTRUCTORS_URL))
                    .andExpect(
                            jsonPath("$.detail").value("The request body is missing or is not valid JSON"));

            verifyNoInteractions(instructorService);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 when the role is not one of the known values")
        void createInstructor_should_return_400_when_role_is_unknown() {
            final String body =
                    """
                    {"fullName": "Laa-Laa", "email": "laalaa@teletubbies.test", "role": "STUDENT"}
                    """;

            mockMvc
                    .perform(
                            post(INSTRUCTORS_URL)
                                    .with(csrf())
                                    .contentType(MediaType.APPLICATION_JSON)
                                    .content(body))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.BAD_REQUEST, TYPE_BASE_URL + "bad-request", INSTRUCTORS_URL))
                    .andExpect(
                            jsonPath("$.detail").value("The request body is missing or is not valid JSON"));

            verifyNoInteractions(instructorService);
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // GET /instructors
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /instructors — list instructors")
    class WhenListingInstructors {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "TEACHER")
        @DisplayName("200 returns both instructors in order with all their fields")
        void getAllInstructors_should_return_200_with_both_instructors_in_order() {
            final String firstId = generateRandomId();
            final String secondId = generateRandomId();
            when(instructorService.getAll())
                    .thenReturn(
                            List.of(
                                    buildInstructor(firstId, FULL_NAME, EMAIL, InstructorRoleType.TEACHER),
                                    buildInstructor(
                                            secondId,
                                            "Po-Po",
                                            "popo@teletubbies.test",
                                            InstructorRoleType.ADMINISTRATOR)));

            mockMvc
                    .perform(get(INSTRUCTORS_URL))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instructors", hasSize(2)))
                    .andExpect(jsonPath("$.instructors[0].id").value(firstId))
                    .andExpect(jsonPath("$.instructors[0].fullName").value(FULL_NAME))
                    .andExpect(jsonPath("$.instructors[0].email").value(EMAIL))
                    .andExpect(jsonPath("$.instructors[0].role").value("TEACHER"))
                    .andExpect(jsonPath("$.instructors[1].id").value(secondId))
                    .andExpect(jsonPath("$.instructors[1].fullName").value("Po-Po"))
                    .andExpect(jsonPath("$.instructors[1].email").value("popo@teletubbies.test"))
                    .andExpect(jsonPath("$.instructors[1].role").value("ADMINISTRATOR"));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // GET /instructors/{instructorId}
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("GET /instructors/{instructorId} — get a single instructor")
    class WhenGettingAnInstructor {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "TEACHER")
        @DisplayName("200 returns the requested instructor")
        void getInstructor_should_return_200_with_the_requested_instructor() {
            final String id = generateRandomId();
            when(instructorService.getById(id)).thenReturn(buildTestInstructor(id));

            mockMvc
                    .perform(get(INSTRUCTOR_URL, id))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instructor.id").value(id))
                    .andExpect(jsonPath("$.instructor.fullName").value(FULL_NAME))
                    .andExpect(jsonPath("$.instructor.email").value(EMAIL))
                    .andExpect(jsonPath("$.instructor.role").value("TEACHER"));
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "TEACHER")
        @DisplayName("404 when the instructor does not exist")
        void getInstructor_should_return_404_when_instructor_does_not_exist() {
            final String id = generateRandomId();
            final String detail = "Instructor not found: " + id;
            when(instructorService.getById(id))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, detail));

            mockMvc
                    .perform(get(INSTRUCTOR_URL, id))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.NOT_FOUND, TYPE_BASE_URL + "not-found", "/instructors/" + id))
                    .andExpect(jsonPath("$.detail").value(detail));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // PUT /instructors/{instructorId}
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("PUT /instructors/{instructorId} — update an instructor")
    class WhenUpdatingAnInstructor {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("200 returns the instructor with the new fields")
        void updateInstructor_should_return_200_with_the_new_fields() {
            final String id = generateRandomId();
            final UpdateInstructorRequest request = buildUpdateInstructorRequestBuilder().build();
            when(instructorService.update(id, request))
                    .thenReturn(
                            buildInstructor(id, "Dipsy", "dipsy@teletubbies.test", InstructorRoleType.TEACHER));

            mockMvc
                    .perform(putUpdate(id, request))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.instructor.id").value(id))
                    .andExpect(jsonPath("$.instructor.fullName").value("Dipsy"))
                    .andExpect(jsonPath("$.instructor.email").value("dipsy@teletubbies.test"))
                    .andExpect(jsonPath("$.instructor.role").value("TEACHER"));
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("400 when the email is malformed")
        void updateInstructor_should_return_400_when_email_is_malformed() {
            final String id = generateRandomId();
            final UpdateInstructorRequest request =
                    buildUpdateInstructorRequestBuilder().email("not-an-email").build();

            mockMvc
                    .perform(putUpdate(id, request))
                    .andExpect(status().isBadRequest())
                    .andExpect(
                            expectValidationProblemDetail(
                                    "/instructors/" + id))
                    .andExpect(jsonPath("$.errors", hasSize(1)))
                    .andExpect(jsonPath("$.errors[0].field").value("email"))
                    .andExpect(
                            jsonPath("$.errors[0].message").value("must be a well-formed email address"));

            verifyNoInteractions(instructorService);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("404 when the instructor does not exist")
        void updateInstructor_should_return_404_when_instructor_does_not_exist() {
            final String id = generateRandomId();
            final String detail = "Instructor not found: " + id;
            when(instructorService.update(any(String.class), any(UpdateInstructorRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, detail));

            mockMvc
                    .perform(putUpdate(id, buildUpdateInstructorRequestBuilder().build()))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.NOT_FOUND, TYPE_BASE_URL + "not-found", "/instructors/" + id))
                    .andExpect(jsonPath("$.detail").value(detail));
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("409 when another instructor already has the full name")
        void updateInstructor_should_return_409_when_full_name_belongs_to_another_instructor() {
            final String id = generateRandomId();
            final String detail = "Instructor full name already exists: Dipsy";
            when(instructorService.update(any(String.class), any(UpdateInstructorRequest.class)))
                    .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, detail));

            mockMvc
                    .perform(putUpdate(id, buildUpdateInstructorRequestBuilder().build()))
                    .andExpect(status().isConflict())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.CONFLICT, TYPE_BASE_URL + "conflict", "/instructors/" + id))
                    .andExpect(jsonPath("$.detail").value(detail));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // DELETE /instructors/{instructorId}
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("DELETE /instructors/{instructorId} — delete an instructor")
    class WhenDeletingAnInstructor {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("204 with an empty body")
        void deleteInstructor_should_return_204_with_empty_body() {
            final String id = generateRandomId();
            doNothing().when(instructorService).delete(id);

            mockMvc
                    .perform(delete(INSTRUCTOR_URL, id).with(csrf()))
                    .andExpect(status().isNoContent())
                    .andExpect(content().string(""));

            verify(instructorService).delete(id);
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("409 when the instructor has assigned courses")
        void deleteInstructor_should_return_409_when_instructor_has_assigned_courses() {
            final String id = generateRandomId();
            final String detail = "Instructor has assigned courses: " + id;
            doThrow(new ResponseStatusException(HttpStatus.CONFLICT, detail))
                    .when(instructorService)
                    .delete(id);

            mockMvc
                    .perform(delete(INSTRUCTOR_URL, id).with(csrf()))
                    .andExpect(status().isConflict())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.CONFLICT, TYPE_BASE_URL + "conflict", "/instructors/" + id))
                    .andExpect(jsonPath("$.detail").value(detail));
        }

        // The single 500 test of the file: an unexpected failure must be reported with a generic
        // message, never with the internal one.
        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("500 without leaking the internal message")
        void deleteInstructor_should_return_500_without_leaking_internal_message() {
            final String id = generateRandomId();
            doThrow(new IllegalStateException("connection to db-internal:5432 refused"))
                    .when(instructorService)
                    .delete(id);

            mockMvc
                    .perform(delete(INSTRUCTOR_URL, id).with(csrf()))
                    .andExpect(status().isInternalServerError())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.INTERNAL_SERVER_ERROR,
                                    TYPE_BASE_URL + "internal-server-error",
                                    "/instructors/" + id))
                    .andExpect(
                            jsonPath("$.detail")
                                    .value("The request could not be processed due to an unexpected error"))
                    .andExpect(content().string(not(containsString("db-internal"))));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // General web framework errors
    // Raised by Spring MVC before any controller method runs; they still have to come back as
    // problem details instead of the container's default error page.
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("General web framework errors")
    class WhenRequestDoesNotMatchAnyHandler {

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("404 for a path no handler maps")
        void unmappedPath_should_return_404() {
            mockMvc
                    .perform(get("/unknown-path"))
                    .andExpect(status().isNotFound())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.NOT_FOUND, TYPE_BASE_URL + "not-found", "/unknown-path"))
                    .andExpect(jsonPath("$.detail").value("The requested endpoint does not exist"));
        }

        @SneakyThrows
        @Test
        @WithMockUser(roles = "ADMINISTRATOR")
        @DisplayName("405 for PATCH /instructors/{instructorId}")
        void patchInstructor_should_return_405() {
            final String id = generateRandomId();

            mockMvc
                    .perform(patch(INSTRUCTOR_URL, id).with(csrf()))
                    .andExpect(status().isMethodNotAllowed())
                    .andExpect(
                            expectProblemDetail(
                                    HttpStatus.METHOD_NOT_ALLOWED,
                                    TYPE_BASE_URL + "method-not-allowed",
                                    "/instructors/" + id))
                    .andExpect(
                            jsonPath("$.detail").value("The HTTP method is not supported by this endpoint"));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Request builders
    // ----------------------------------------------------------------------------------------------------

    private MockHttpServletRequestBuilder postCreate(
            final NewInstructorRequest request) throws Exception {
        return post(INSTRUCTORS_URL)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    private MockHttpServletRequestBuilder putUpdate(
            final String id, final UpdateInstructorRequest request) throws Exception {
        return put(INSTRUCTOR_URL, id)
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request));
    }

    // ----------------------------------------------------------------------------------------------------
    // Test data helpers
    // The two request builders return the Builder (not the request) so each test can override the
    // single field it is about. A generated Builder is single-use: always ask for a fresh one.
    // ----------------------------------------------------------------------------------------------------

    private static String generateRandomId() {
        return UUID.randomUUID().toString();
    }

    private static InstructorResource buildInstructor(
            final String id, final String fullName, final String email, final InstructorRoleType role) {
        return InstructorResource.builder().id(id).fullName(fullName).email(email).role(role).build();
    }

    private static InstructorResource buildTestInstructor(final String id) {
        return buildInstructor(id, FULL_NAME, EMAIL, InstructorRoleType.TEACHER);
    }

    private static NewInstructorRequest.Builder buildNewInstructorRequestBuilder() {
        return NewInstructorRequest.builder()
                .fullName(FULL_NAME)
                .email(EMAIL)
                .password(PASSWORD)
                .role(InstructorRoleType.TEACHER);
    }

    private static UpdateInstructorRequest.Builder buildUpdateInstructorRequestBuilder() {
        return UpdateInstructorRequest.builder().fullName("Dipsy").email("dipsy@teletubbies.test");
    }

    // ----------------------------------------------------------------------------------------------------
    // ProblemDetail matchers
    // Duplicated per class on purpose: the repository has no shared base class. MockMvc needs a
    // ResultMatcher (a functional interface), so these cannot be a plain Consumer<MvcResult>, but
    // their bodies are pure AssertJ. SUPPRESS_EXCEPTIONS makes a missing property read as null
    // instead of throwing PathNotFoundException, which is what lets us assert absence.
    // ----------------------------------------------------------------------------------------------------

    /** Common problem shape; {@code errors} must NOT be present. */
    private static ResultMatcher expectProblemDetail(
            final HttpStatus status, final String type, final String instance) {
        return result ->
                assertThat(assertProblem(result, status, type, instance).<Object>read("errors"))
                        .as("errors")
                        .isNull();
    }

    /** Common problem shape; {@code errors} MUST be present (validation failures). */
    private static ResultMatcher expectValidationProblemDetail(
            final String instance) {
        return result ->
                assertThat(assertProblem(result, HttpStatus.BAD_REQUEST, "https://api.teletubbies.dev/problems/bad-request", instance).<Object>read("errors"))
                        .as("errors")
                        .isNotNull();
    }

    private static DocumentContext assertProblem(
            final MvcResult result, final HttpStatus status, final String type, final String instance)
            throws Exception {
        final DocumentContext json =
                JsonPath.using(Configuration.defaultConfiguration().addOptions(Option.SUPPRESS_EXCEPTIONS))
                        .parse(result.getResponse().getContentAsString(StandardCharsets.UTF_8));
        assertThat(json.<Integer>read("status")).as("status").isEqualTo(status.value());
        assertThat(json.<String>read("type")).as("type").isEqualTo(type);
        assertThat(json.<String>read("instance")).as("instance").isEqualTo(instance);
        assertThat(json.<String>read("detail")).as("detail").isNotBlank();
        assertThat(json.<String>read("traceId")).as("traceId").matches(TRACE_ID_REGEX);
        return json;
    }
}