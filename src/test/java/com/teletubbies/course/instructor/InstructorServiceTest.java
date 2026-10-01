package com.teletubbies.course.instructor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.teletubbies.course.course.CourseRepository;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.assertj.core.api.ThrowableAssert.ThrowingCallable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link InstructorService}.
 *
 * <p>The three collaborators ({@link InstructorRepository}, {@link CourseRepository} and {@link
 * PasswordEncoder}) are mocks, so every assertion is about the behaviour of the service itself.
 * Expectations are derived from the fixture constants, never from the entities used to stub the
 * repository, so a wrong mapping cannot pass unnoticed.
 */
@ExtendWith(MockitoExtension.class)
class InstructorServiceTest {

    // ----------------------------------------------------------------------------------------------------
    // Fixtures
    // Fixed UUIDs in the same range as the DBUnit dataset, so failures are easy to read.
    // ----------------------------------------------------------------------------------------------------

    private record InstructorFixture(
            UUID id, String fullName, String email, String passwordHash, InstructorRoleType role) {}

    private static final InstructorFixture INSTRUCTOR =
            new InstructorFixture(
                    UUID.fromString("00000000-0000-0000-0000-000000000001"),
                    "Laa-Laa",
                    "laalaa@teletubbies.test",
                    "hash",
                    InstructorRoleType.TEACHER);

    private static final InstructorFixture OTHER_INSTRUCTOR =
            new InstructorFixture(
                    UUID.fromString("00000000-0000-0000-0000-000000000002"),
                    "Po-Po",
                    "popo@teletubbies.test",
                    "hash",
                    InstructorRoleType.ADMINISTRATOR);

    // Same id and role as INSTRUCTOR: only the name and the email change on update.
    private static final InstructorFixture UPDATED_INSTRUCTOR =
            new InstructorFixture(
                    INSTRUCTOR.id(),
                    "Dipsy",
                    "dipsy@teletubbies.test",
                    INSTRUCTOR.passwordHash(),
                    INSTRUCTOR.role());

    private static final String PLAIN_PASSWORD = "s3cret-pass";

    @Mock private InstructorRepository instructorRepository;
    @Mock private CourseRepository courseRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks private InstructorService instructorService;

    // ----------------------------------------------------------------------------------------------------
    // getAll()
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("getAll()")
    class GetAll {

        @Test
        @DisplayName("maps every instructor together with its role")
        void getAll_should_map_every_instructor_with_its_role() {
            final Pageable pageable = PageRequest.of(0, 20);
            when(instructorRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(
                            new PageImpl<>(
                                    List.of(buildInstructorEntity(INSTRUCTOR), buildInstructorEntity(OTHER_INSTRUCTOR)),
                                    pageable,
                                    2));

            final Page<InstructorResource> result =
                    instructorService.getAll(null, null, null, pageable);

            assertThat(result.getContent())
                    .containsExactly(
                            buildInstructorResource(INSTRUCTOR), buildInstructorResource(OTHER_INSTRUCTOR));
        }

        @Test
        @DisplayName("returns an empty page without touching courses or the password encoder")
        void getAll_should_return_empty_page_without_touching_other_collaborators() {
            final Pageable pageable = PageRequest.of(0, 20);
            when(instructorRepository.findAll(any(Specification.class), eq(pageable)))
                    .thenReturn(Page.empty(pageable));

            final Page<InstructorResource> result =
                    instructorService.getAll(null, null, null, pageable);

            assertThat(result).isEmpty();
            verifyNoInteractions(courseRepository, passwordEncoder);
        }

        @Test
        @DisplayName("falls back to an unpaged request when pageable is null")
        void getAll_should_useUnpagedRequest_when_pageableIsNull() {
            when(instructorRepository.findAll(any(Specification.class), eq(Pageable.unpaged())))
                    .thenReturn(Page.empty());

            instructorService.getAll(null, null, null, null);

            verify(instructorRepository).findAll(any(Specification.class), eq(Pageable.unpaged()));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // getById(String)
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("getById(String)")
    class GetById {

        @Test
        @DisplayName("maps the requested instructor")
        void getById_should_map_the_requested_instructor() {
            when(instructorRepository.findById(OTHER_INSTRUCTOR.id()))
                    .thenReturn(Optional.of(buildInstructorEntity(OTHER_INSTRUCTOR)));

            final InstructorResource result = instructorService.getById(OTHER_INSTRUCTOR.id().toString());

            assertThat(result).isEqualTo(buildInstructorResource(OTHER_INSTRUCTOR));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.teletubbies.course.instructor.InstructorServiceTest#provideUnparsableIds")
        @DisplayName("404 when the id is not a UUID")
        void getById_should_fail_with_404_when_id_is_not_parsable(
                final String testCase, final String id) {
            assertFailsWith(
                    testCase,
                    () -> instructorService.getById(id),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + id);

            verifyNoInteractions(instructorRepository);
        }

        @Test
        @DisplayName("404 when no instructor has that id")
        void getById_should_fail_with_404_when_instructor_does_not_exist() {
            final InstructorFixture missing = copyWithId(uuid());
            when(instructorRepository.findById(missing.id())).thenReturn(Optional.empty());

            assertFailsWith(
                    () -> instructorService.getById(missing.id().toString()),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + missing.id());
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // create(NewInstructorRequest)
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("create(NewInstructorRequest)")
    class Create {

        @Test
        @DisplayName("persists the encoded password and the requested role")
        void create_should_persist_encoded_password_and_requested_role() {
            final NewInstructorRequest request =
                    buildNewInstructorRequest(OTHER_INSTRUCTOR, PLAIN_PASSWORD, OTHER_INSTRUCTOR.role());
            when(passwordEncoder.encode(PLAIN_PASSWORD)).thenReturn(OTHER_INSTRUCTOR.passwordHash());
            when(instructorRepository.save(any(InstructorEntity.class)))
                    .thenAnswer(
                            invocation ->
                                    withId(invocation.getArgument(0), OTHER_INSTRUCTOR.id()));

            final InstructorResource result = instructorService.create(request);

            final ArgumentCaptor<InstructorEntity> saved =
                    ArgumentCaptor.forClass(InstructorEntity.class);
            verify(instructorRepository).save(saved.capture());
            assertThat(saved.getValue())
                    .extracting(
                            InstructorEntity::getFullName,
                            InstructorEntity::getEmail,
                            InstructorEntity::getPassword,
                            InstructorEntity::getRole)
                    .containsExactly(
                            OTHER_INSTRUCTOR.fullName(),
                            OTHER_INSTRUCTOR.email(),
                            OTHER_INSTRUCTOR.passwordHash(),
                            OTHER_INSTRUCTOR.role());
            assertThat(result).isEqualTo(buildInstructorResource(OTHER_INSTRUCTOR));
        }

        @Test
        @DisplayName("stores a null password and never calls the encoder when none is sent")
        void create_should_store_null_password_when_request_has_none() {
            final NewInstructorRequest request =
                    buildNewInstructorRequest(INSTRUCTOR, null, INSTRUCTOR.role());
            when(instructorRepository.save(any(InstructorEntity.class)))
                    .thenAnswer(
                            invocation -> withId(invocation.getArgument(0), INSTRUCTOR.id()));

            instructorService.create(request);

            final ArgumentCaptor<InstructorEntity> saved =
                    ArgumentCaptor.forClass(InstructorEntity.class);
            verify(instructorRepository).save(saved.capture());
            assertThat(saved.getValue().getPassword()).isNull();
            verifyNoInteractions(passwordEncoder);
        }

        @Test
        @DisplayName("defaults the role to TEACHER when the request has none")
        void create_should_default_role_to_teacher() {
            // OTHER_INSTRUCTOR is an ADMINISTRATOR: the TEACHER below cannot come from the fixture.
            final NewInstructorRequest request = buildNewInstructorRequest(OTHER_INSTRUCTOR, null, null);
            when(instructorRepository.save(any(InstructorEntity.class)))
                    .thenAnswer(
                            invocation ->
                                    withId(invocation.getArgument(0), OTHER_INSTRUCTOR.id()));

            final InstructorResource result = instructorService.create(request);

            assertThat(result.getRole()).isEqualTo(InstructorRoleType.TEACHER);
        }

        @Test
        @DisplayName("409 when the full name already exists")
        void create_should_fail_with_409_when_full_name_already_exists() {
            final NewInstructorRequest request =
                    buildNewInstructorRequest(INSTRUCTOR, PLAIN_PASSWORD, INSTRUCTOR.role());
            when(instructorRepository.existsByFullName(INSTRUCTOR.fullName())).thenReturn(true);

            assertFailsWith(
                    () -> instructorService.create(request),
                    HttpStatus.CONFLICT,
                    "Instructor full name already exists: " + INSTRUCTOR.fullName());

            verify(instructorRepository, never()).save(any(InstructorEntity.class));
            verifyNoInteractions(courseRepository, passwordEncoder);
        }

        @Test
        @DisplayName("409 when the email already exists and the name is free")
        void create_should_fail_with_409_when_email_already_exists() {
            final NewInstructorRequest request =
                    buildNewInstructorRequest(INSTRUCTOR, PLAIN_PASSWORD, INSTRUCTOR.role());
            when(instructorRepository.existsByEmail(INSTRUCTOR.email())).thenReturn(true);

            assertFailsWith(
                    () -> instructorService.create(request),
                    HttpStatus.CONFLICT,
                    "Instructor email already exists: " + INSTRUCTOR.email());

            // The hash is computed only after both checks pass: no point paying for BCrypt otherwise.
            verify(instructorRepository, never()).save(any(InstructorEntity.class));
            verifyNoInteractions(courseRepository, passwordEncoder);
        }

        @Test
        @DisplayName("evaluates the full name conflict before the email conflict")
        void create_should_evaluate_full_name_conflict_before_email_conflict() {
            final NewInstructorRequest request =
                    buildNewInstructorRequest(INSTRUCTOR, PLAIN_PASSWORD, INSTRUCTOR.role());
            when(instructorRepository.existsByFullName(INSTRUCTOR.fullName())).thenReturn(true);
            // Both fields conflict, but the email must never be looked at: lenient on purpose.
            lenient().when(instructorRepository.existsByEmail(INSTRUCTOR.email())).thenReturn(true);

            assertFailsWith(
                    () -> instructorService.create(request),
                    HttpStatus.CONFLICT,
                    "Instructor full name already exists: " + INSTRUCTOR.fullName());

            verify(instructorRepository, never()).existsByEmail(any(String.class));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // update(String, UpdateInstructorRequest)
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("update(String, UpdateInstructorRequest)")
    class Update {

        @Test
        @DisplayName("applies name and email and saves, leaving role and password untouched")
        void update_should_apply_name_and_email_leaving_role_and_password_untouched() {
            final InstructorEntity instructor = buildInstructorEntity(INSTRUCTOR);
            when(instructorRepository.findById(INSTRUCTOR.id())).thenReturn(Optional.of(instructor));
            when(instructorRepository.save(instructor)).thenReturn(instructor);

            final InstructorResource result =
                    instructorService.update(
                            INSTRUCTOR.id().toString(), buildUpdateInstructorRequest());

            final ArgumentCaptor<InstructorEntity> saved =
                    ArgumentCaptor.forClass(InstructorEntity.class);
            verify(instructorRepository).save(saved.capture());
            assertThat(saved.getValue())
                    .extracting(
                            InstructorEntity::getFullName,
                            InstructorEntity::getEmail,
                            InstructorEntity::getPassword,
                            InstructorEntity::getRole)
                    .containsExactly(
                            UPDATED_INSTRUCTOR.fullName(),
                            UPDATED_INSTRUCTOR.email(),
                            INSTRUCTOR.passwordHash(),
                            INSTRUCTOR.role());
            assertThat(result).isEqualTo(buildInstructorResource(UPDATED_INSTRUCTOR));
        }

        @Test
        @DisplayName("409 when another instructor has the full name, entity left untouched")
        void update_should_fail_with_409_when_another_instructor_has_the_full_name() {
            final InstructorEntity instructor = buildInstructorEntity(INSTRUCTOR);
            when(instructorRepository.findById(INSTRUCTOR.id())).thenReturn(Optional.of(instructor));
            when(instructorRepository.existsByFullNameAndIdNot(
                    UPDATED_INSTRUCTOR.fullName(), INSTRUCTOR.id()))
                    .thenReturn(true);

            assertFailsWith(
                    () ->
                            instructorService.update(
                                    INSTRUCTOR.id().toString(), buildUpdateInstructorRequest()),
                    HttpStatus.CONFLICT,
                    "Instructor full name already exists: " + UPDATED_INSTRUCTOR.fullName());

            assertThat(instructor.getFullName()).isEqualTo(INSTRUCTOR.fullName());
            assertThat(instructor.getEmail()).isEqualTo(INSTRUCTOR.email());
            verify(instructorRepository, never()).save(any(InstructorEntity.class));
        }

        @Test
        @DisplayName("409 when another instructor has the email, entity left untouched")
        void update_should_fail_with_409_when_another_instructor_has_the_email() {
            final InstructorEntity instructor = buildInstructorEntity(INSTRUCTOR);
            when(instructorRepository.findById(INSTRUCTOR.id())).thenReturn(Optional.of(instructor));
            when(instructorRepository.existsByEmailAndIdNot(UPDATED_INSTRUCTOR.email(), INSTRUCTOR.id()))
                    .thenReturn(true);

            assertFailsWith(
                    () ->
                            instructorService.update(
                                    INSTRUCTOR.id().toString(), buildUpdateInstructorRequest()),
                    HttpStatus.CONFLICT,
                    "Instructor email already exists: " + UPDATED_INSTRUCTOR.email());

            assertThat(instructor.getFullName()).isEqualTo(INSTRUCTOR.fullName());
            assertThat(instructor.getEmail()).isEqualTo(INSTRUCTOR.email());
            verify(instructorRepository, never()).save(any(InstructorEntity.class));
        }

        @Test
        @DisplayName("evaluates the full name conflict before the email conflict")
        void update_should_evaluate_full_name_conflict_before_email_conflict() {
            final InstructorEntity instructor = buildInstructorEntity(INSTRUCTOR);
            when(instructorRepository.findById(INSTRUCTOR.id())).thenReturn(Optional.of(instructor));
            when(instructorRepository.existsByFullNameAndIdNot(
                    UPDATED_INSTRUCTOR.fullName(), INSTRUCTOR.id()))
                    .thenReturn(true);
            // Both fields conflict, but the email must never be looked at: lenient on purpose.
            lenient()
                    .when(
                            instructorRepository.existsByEmailAndIdNot(
                                    UPDATED_INSTRUCTOR.email(), INSTRUCTOR.id()))
                    .thenReturn(true);

            assertFailsWith(
                    () ->
                            instructorService.update(
                                    INSTRUCTOR.id().toString(), buildUpdateInstructorRequest()),
                    HttpStatus.CONFLICT,
                    "Instructor full name already exists: " + UPDATED_INSTRUCTOR.fullName());

            verify(instructorRepository, never())
                    .existsByEmailAndIdNot(any(String.class), any(UUID.class));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.teletubbies.course.instructor.InstructorServiceTest#provideUnparsableIds")
        @DisplayName("404 when the id is not a UUID")
        void update_should_fail_with_404_when_id_is_not_parsable(
                final String testCase, final String id) {
            assertFailsWith(
                    testCase,
                    () -> instructorService.update(id, buildUpdateInstructorRequest()),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + id);

            verifyNoInteractions(instructorRepository);
        }

        @Test
        @DisplayName("404 when no instructor has that id")
        void update_should_fail_with_404_when_instructor_does_not_exist() {
            final InstructorFixture missing = copyWithId(uuid());
            when(instructorRepository.findById(missing.id())).thenReturn(Optional.empty());

            assertFailsWith(
                    () ->
                            instructorService.update(
                                    missing.id().toString(), buildUpdateInstructorRequest()),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + missing.id());

            verify(instructorRepository, never()).save(any(InstructorEntity.class));
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // delete(String)
    // ----------------------------------------------------------------------------------------------------

    @Nested
    @DisplayName("delete(String)")
    class Delete {

        @Test
        @DisplayName("deletes the instructor when it has no assigned courses")
        void delete_should_delete_when_instructor_has_no_courses() {
            final InstructorEntity instructor = buildInstructorEntity(INSTRUCTOR);
            when(instructorRepository.findById(INSTRUCTOR.id())).thenReturn(Optional.of(instructor));
            when(courseRepository.existsByInstructorId(INSTRUCTOR.id())).thenReturn(false);

            instructorService.delete(INSTRUCTOR.id().toString());

            verify(instructorRepository).delete(instructor);
        }

        @Test
        @DisplayName("409 when the instructor has assigned courses")
        void delete_should_fail_with_409_when_instructor_has_assigned_courses() {
            when(instructorRepository.findById(INSTRUCTOR.id()))
                    .thenReturn(Optional.of(buildInstructorEntity(INSTRUCTOR)));
            when(courseRepository.existsByInstructorId(INSTRUCTOR.id())).thenReturn(true);

            assertFailsWith(
                    () -> instructorService.delete(INSTRUCTOR.id().toString()),
                    HttpStatus.CONFLICT,
                    "Instructor has assigned courses: " + INSTRUCTOR.id());

            verify(instructorRepository, never()).delete(any(InstructorEntity.class));
        }

        @ParameterizedTest(name = "{0}")
        @MethodSource("com.teletubbies.course.instructor.InstructorServiceTest#provideUnparsableIds")
        @DisplayName("404 when the id is not a UUID")
        void delete_should_fail_with_404_when_id_is_not_parsable(
                final String testCase, final String id) {
            assertFailsWith(
                    testCase,
                    () -> instructorService.delete(id),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + id);

            verifyNoInteractions(instructorRepository, courseRepository);
        }

        @Test
        @DisplayName("404 when no instructor has that id")
        void delete_should_fail_with_404_when_instructor_does_not_exist() {
            final InstructorFixture missing = copyWithId(uuid());
            when(instructorRepository.findById(missing.id())).thenReturn(Optional.empty());

            assertFailsWith(
                    () -> instructorService.delete(missing.id().toString()),
                    HttpStatus.NOT_FOUND,
                    "Instructor not found: " + missing.id());

            verify(instructorRepository, never()).delete(any(InstructorEntity.class));
            verifyNoInteractions(courseRepository);
        }
    }

    // ----------------------------------------------------------------------------------------------------
    // Shared data providers
    // Lives in the OUTER class: the three @Nested groups reference it by fully qualified name.
    // The 35-character UUID with a short last group is deliberately absent: UUID.fromString is
    // lenient and normalises it, so it is valid input. Non-hex characters raise
    // NumberFormatException, a subclass of IllegalArgumentException, which idOrThrow already
    // catches.
    // ----------------------------------------------------------------------------------------------------

    static Stream<Arguments> provideUnparsableIds() {
        return Stream.of(
                Arguments.of("empty string", ""),
                Arguments.of("blank string", " "),
                Arguments.of("free text", "not-a-uuid"),
                Arguments.of("digits only", "12345"),
                Arguments.of("too few groups", "00000000-0000-0000-0000"),
                Arguments.of("too many groups", "00000000-0000-0000-0000-000000000101-102"),
                Arguments.of("non-hex character", "00000000-0000-0000-0000-00000000010Z"));
    }

    // ----------------------------------------------------------------------------------------------------
    // Assertion helpers
    // A single place for the "ResponseStatusException with status X and message Y" check, so each
    // test reads as one line of intent.
    // ----------------------------------------------------------------------------------------------------

    private static void assertFailsWith(
            final ThrowingCallable call, final HttpStatus status, final String message) {
        assertFailsWith(status + " " + message, call, status, message);
    }

    private static void assertFailsWith(
            final String description,
            final ThrowingCallable call,
            final HttpStatus status,
            final String message) {
        assertThatThrownBy(call)
                .as(description)
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining(message)
                .extracting(e -> ((ResponseStatusException) e).getStatusCode())
                .isEqualTo(status);
    }

    // ----------------------------------------------------------------------------------------------------
    // Builders: fixture -> entity / request / resource
    // The entity is built with the four-argument constructor so the arrange step does not depend on
    // the (request, hash) constructor that create() exercises.
    // ----------------------------------------------------------------------------------------------------

    private static InstructorEntity buildInstructorEntity(final InstructorFixture fixture) {
        final InstructorEntity entity =
                new InstructorEntity(
                        fixture.fullName(), fixture.email(), fixture.passwordHash(), fixture.role());
        return withId(entity, fixture.id());
    }

    // The id is generated by JPA, so there is no setter: reflection stands in for the database.
    private static InstructorEntity withId(final InstructorEntity entity, final UUID id) {
        entity.setId(id);
        return entity;
    }

    private static NewInstructorRequest buildNewInstructorRequest(
            final InstructorFixture fixture, final String password, final InstructorRoleType role) {
        return NewInstructorRequest.builder()
                .fullName(fixture.fullName())
                .email(fixture.email())
                .password(password)
                .role(role)
                .build();
    }

    private static UpdateInstructorRequest buildUpdateInstructorRequest() {
        return UpdateInstructorRequest.builder()
                .fullName(InstructorServiceTest.UPDATED_INSTRUCTOR.fullName())
                .email(InstructorServiceTest.UPDATED_INSTRUCTOR.email())
                .build();
    }

    private static InstructorResource buildInstructorResource(final InstructorFixture fixture) {
        return InstructorResource.builder()
                .id(fixture.id().toString())
                .fullName(fixture.fullName())
                .email(fixture.email())
                .role(fixture.role())
                .build();
    }

    private static InstructorFixture copyWithId(final UUID id) {
        return new InstructorFixture(
                id, InstructorServiceTest.INSTRUCTOR.fullName(), InstructorServiceTest.INSTRUCTOR.email(), InstructorServiceTest.INSTRUCTOR.passwordHash(), InstructorServiceTest.INSTRUCTOR.role());
    }

    private static UUID uuid() {
        return UUID.fromString(String.format("00000000-0000-0000-0000-%012d", 99));
    }
}