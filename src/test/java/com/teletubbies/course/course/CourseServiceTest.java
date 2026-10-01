package com.teletubbies.course.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import com.teletubbies.course.instructor.InstructorEntity;
import com.teletubbies.course.instructor.InstructorRepository;
import com.teletubbies.course.instructor.InstructorRoleType;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
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
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

/**
 * Unit tests for {@link CourseService}: both collaborators are mocks, so every assertion here is
 * about the behaviour of the service and nothing else.
 *
 * <p>Expected values come from the {@code *_fixture} records instead of from the entities built to
 * stub the repository: taking the expectation from the mapper output would let a wrong mapping pass
 * unnoticed.
 *
 * <p>Tests are grouped with {@code @Nested} by the service method under test
 */
@ExtendWith(MockitoExtension.class)
class CourseServiceTest {

  /** Fixed values, so any accidental change in the mapping shows up as a failed assertion. */
  private record InstructorFixture(UUID id, String fullName, String email, InstructorRoleType role) {}

  private record CourseFixture(
          UUID id,
          String name,
          String description,
          int duration,
          CourseLevelType level,
          CourseCategoryType category,
          InstructorFixture instructor) {}

  private static final InstructorFixture INSTRUCTOR =
          new InstructorFixture(
                  UUID.fromString("00000000-0000-0000-0000-000000000001"),
                  "Laa-Laa",
                  "laalaa@teletubbies.test",
                  InstructorRoleType.TEACHER);

  private static final InstructorFixture OTHER_INSTRUCTOR =
          new InstructorFixture(
                  UUID.fromString("00000000-0000-0000-0000-000000000002"),
                  "Po-Po",
                  "popo@teletubbies.test",
                  InstructorRoleType.ADMINISTRATOR);

  private static final CourseFixture COURSE =
          new CourseFixture(
                  UUID.fromString("00000000-0000-0000-0000-000000000101"),
                  "Introduction to Spring Boot",
                  "Learn the fundamentals of building REST APIs with Spring Boot.",
                  12,
                  CourseLevelType.BEGINNER,
                  CourseCategoryType.PROGRAMMING,
                  INSTRUCTOR);

  private static final CourseFixture OTHER_COURSE =
          new CourseFixture(
                  UUID.fromString("00000000-0000-0000-0000-000000000102"),
                  "UI Design with Figma",
                  "UI/UX design principles applied with Figma.",
                  8,
                  CourseLevelType.INTERMEDIATE,
                  CourseCategoryType.DESIGN,
                  OTHER_INSTRUCTOR);

  private static final CourseFixture UPDATED_COURSE =
          new CourseFixture(
                  COURSE.id(),
                  "Spring boot course",
                  "IoC, web and persistence.",
                  4,
                  CourseLevelType.ADVANCED,
                  CourseCategoryType.SCIENCE,
                  OTHER_INSTRUCTOR);

  @Mock private CourseRepository courseRepository;
  @Mock private InstructorRepository instructorRepository;
  @InjectMocks private CourseService courseService;

  // ---------------------------------------------------------------------------------------------
  // getAll()
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("getAll()")
  class GetAll {

    @Test
    @DisplayName("maps every course together with its instructor")
    void mapsEveryCourseWithItsInstructor() {
      // GIVEN
      when(courseRepository.findAll())
              .thenReturn(List.of(buildCourseEntity(COURSE), buildCourseEntity(OTHER_COURSE)));

      // WHEN
      final List<CourseResource> result = courseService.getAll();

      // THEN
      assertThat(result).containsExactly(buildCourseResource(COURSE), buildCourseResource(OTHER_COURSE));
      verify(courseRepository).findAll();
      verifyNoMoreInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @Test
    @DisplayName("returns an empty list when there are no courses")
    void returnsAnEmptyListWhenThereAreNoCourses() {
      // GIVEN
      when(courseRepository.findAll()).thenReturn(List.of());

      // WHEN / THEN
      assertThat(courseService.getAll()).isEmpty();
      verifyNoInteractions(instructorRepository);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // getById(String)
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("getById(String)")
  class GetById {

    @Test
    @DisplayName("returns the course together with its instructor")
    void returnsTheCourseWithItsInstructor() {
      // GIVEN
      when(courseRepository.findById(any(UUID.class)))
              .thenReturn(Optional.of(buildCourseEntity(COURSE)));

      // WHEN
      final CourseResource result = courseService.getById(COURSE.id().toString());

      // THEN
      assertThat(result).isEqualTo(buildCourseResource(COURSE));
      verify(courseRepository).findById(any(UUID.class));
      verifyNoMoreInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @ParameterizedTest
    @MethodSource("com.teletubbies.course.course.CourseServiceTest#provideUnparsableCourseIds")
    @DisplayName("throws 404 when the id is not a valid UUID")
    void throwsNotFoundWhenIdIsNotAUuid(final String testCase, final String courseId) {
      // WHEN / THEN
      assertFailsWith(
              testCase,
              () -> courseService.getById(courseId),
              HttpStatus.NOT_FOUND,
              "Course not found: " + courseId);
      verifyNoInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @Test
    @DisplayName("throws 404 when the course does not exist")
    void throwsNotFoundWhenCourseDoesNotExist() {
      // GIVEN
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.empty());

      // WHEN / THEN
      assertFailsWith(
              "existing id, missing row",
              () -> courseService.getById(COURSE.id().toString()),
              HttpStatus.NOT_FOUND,
              "Course not found: " + COURSE.id());

      verify(courseRepository).findById(COURSE.id());
      verifyNoMoreInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // create(NewCourseRequest)
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("create(NewCourseRequest)")
  class Create {

    @Test
    @DisplayName("persists the course with the instructor it was given")
    void persistsTheCourseWithTheInstructorItWasGiven() {
      // GIVEN
      final NewCourseRequest request = buildNewCourseRequest(COURSE);
      final InstructorEntity instructor = buildInstructorEntity(COURSE.instructor());
      final UUID generatedId = UUID.randomUUID();

      when(courseRepository.existsByName(COURSE.name())).thenReturn(false);
      when(instructorRepository.findById(COURSE.instructor().id()))
              .thenReturn(Optional.of(instructor));
      when(courseRepository.save(any(CourseEntity.class)))
              .thenAnswer(
                      invocation -> {
                        final CourseEntity saved = invocation.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", generatedId); // the database generates the id
                        return saved;
                      });

      // WHEN
      final CourseResource result = courseService.create(request);

      // THEN
      assertThat(result).isEqualTo(buildCourseResource(copyWithId(generatedId)));

      final ArgumentCaptor<CourseEntity> saved = ArgumentCaptor.forClass(CourseEntity.class);
      verify(courseRepository).save(saved.capture());
      verify(courseRepository).existsByName(COURSE.name());
      verifyNoMoreInteractions(courseRepository);
      verify(instructorRepository).findById(COURSE.instructor().id());
      verifyNoMoreInteractions(instructorRepository);

      assertThat(saved.getValue())
              .extracting(
                      CourseEntity::getName,
                      CourseEntity::getDescription,
                      CourseEntity::getDuration,
                      CourseEntity::getLevel,
                      CourseEntity::getCategory,
                      CourseEntity::getInstructorId)
              .containsExactly(
                      COURSE.name(),
                      COURSE.description(),
                      (short) COURSE.duration(),
                      COURSE.level(),
                      COURSE.category(),
                      COURSE.instructor().id());
      // The association is what feeds the response, so the persisted course has to carry it.
      assertThat(saved.getValue().getInstructor()).isSameAs(instructor);
    }

    @Test
    @DisplayName("throws 409 when the name already exists")
    void throwsConflictWhenNameAlreadyExists() {
      // GIVEN
      when(courseRepository.existsByName(COURSE.name())).thenReturn(true);

      // WHEN / THEN
      assertFailsWith(
              "duplicated name",
              () -> courseService.create(buildNewCourseRequest(COURSE)),
              HttpStatus.CONFLICT,
              "Course name already exists: " + COURSE.name());
      verify(courseRepository).existsByName(COURSE.name());
      verify(courseRepository, never()).save(any(CourseEntity.class));
      // The name is rejected before resolving any instructor.
      verifyNoInteractions(instructorRepository);
    }

    @ParameterizedTest
    @MethodSource("com.teletubbies.course.course.CourseServiceTest#provideInvalidInstructorIds")
    @DisplayName("throws 400 when the instructor id is not a valid UUID")
    void throwsBadRequestWhenInstructorIdIsNotAUuid(final String testCase, final String instructorId) {
      // GIVEN
      when(courseRepository.existsByName(COURSE.name())).thenReturn(false);

      // WHEN / THEN
      assertFailsWith(
              testCase,
              () -> courseService.create(buildNewCourseRequest(COURSE, instructorId)),
              HttpStatus.BAD_REQUEST,
              "Invalid instructorId: " + instructorId);
      verifyNoInteractions(instructorRepository);
      verify(courseRepository, never()).save(any(CourseEntity.class));
    }

    @Test
    @DisplayName("throws 404 when the instructor does not exist")
    void throwsNotFoundWhenInstructorDoesNotExist() {
      // GIVEN
      when(courseRepository.existsByName(COURSE.name())).thenReturn(false);
      when(instructorRepository.findById(COURSE.instructor().id())).thenReturn(Optional.empty());

      // WHEN / THEN
      assertFailsWith(
              "unknown instructor",
              () -> courseService.create(buildNewCourseRequest(COURSE)),
              HttpStatus.NOT_FOUND,
              "Instructor not found: " + COURSE.instructor().id());
      verify(courseRepository).existsByName(COURSE.name());
      verify(courseRepository, never()).save(any(CourseEntity.class));
    }
  }

  // ---------------------------------------------------------------------------------------------
  // update(String, UpdateCourseRequest)
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("update(String, UpdateCourseRequest)")
  class Update {

    @Test
    @DisplayName("applies every field and reassigns the instructor")
    void appliesEveryFieldAndReassignsTheInstructor() {
      // GIVEN
      final CourseEntity course = buildCourseEntity(COURSE);
      final InstructorEntity newInstructor = buildInstructorEntity(UPDATED_COURSE.instructor());

      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.of(course));
      when(courseRepository.existsByNameAndIdNot(UPDATED_COURSE.name(), COURSE.id())).thenReturn(false);
      when(instructorRepository.findById(UPDATED_COURSE.instructor().id()))
              .thenReturn(Optional.of(newInstructor));
      when(courseRepository.save(course)).thenReturn(course);

      // WHEN
      final CourseResource result =
              courseService.update(COURSE.id().toString(), buildUpdateCourseRequest(UPDATED_COURSE));

      // THEN
      assertThat(result).isEqualTo(buildCourseResource(UPDATED_COURSE));
      assertThat(course.getId()).isEqualTo(COURSE.id());
      // The association follows the request: otherwise the response keeps the previous instructor.
      assertThat(course.getInstructor()).isSameAs(newInstructor);
      assertThat(course.getInstructorId()).isEqualTo(UPDATED_COURSE.instructor().id());

      verify(courseRepository).save(course);
      verifyNoMoreInteractions(courseRepository);
      verify(instructorRepository).findById(UPDATED_COURSE.instructor().id());
      verifyNoMoreInteractions(instructorRepository);
    }

    @Test
    @DisplayName("throws 409 when another course already uses the name")
    void throwsConflictWhenAnotherCourseAlreadyUsesTheName() {
      // GIVEN
      final CourseEntity course = buildCourseEntity(COURSE);
      final InstructorEntity instructor = course.getInstructor();

      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.of(course));
      when(courseRepository.existsByNameAndIdNot(UPDATED_COURSE.name(), COURSE.id())).thenReturn(true);

      // WHEN / THEN
      assertFailsWith(
              "duplicated name",
              () -> courseService.update(COURSE.id().toString(), buildUpdateCourseRequest(UPDATED_COURSE)),
              HttpStatus.CONFLICT,
              "Course name already exists: " + UPDATED_COURSE.name());
      verify(courseRepository, never()).save(any(CourseEntity.class));
      verifyNoInteractions(instructorRepository);
      // The conflict is detected before applying anything, so the course is left untouched.
      assertThat(course.getName()).isEqualTo(COURSE.name());
      assertThat(course.getInstructor()).isSameAs(instructor);
    }

    @ParameterizedTest
    @MethodSource("com.teletubbies.course.course.CourseServiceTest#provideInvalidInstructorIds")
    @DisplayName("throws 400 when the instructor id is not a valid UUID")
    void throwsBadRequestWhenInstructorIdIsNotAUuid(final String testCase, final String instructorId) {
      // GIVEN
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.of(buildCourseEntity(COURSE)));
      when(courseRepository.existsByNameAndIdNot(UPDATED_COURSE.name(), COURSE.id())).thenReturn(false);

      // WHEN / THEN
      assertFailsWith(
              testCase,
              () ->
                      courseService.update(
                              COURSE.id().toString(), buildUpdateCourseRequest(UPDATED_COURSE, instructorId)),
              HttpStatus.BAD_REQUEST,
              "Invalid instructorId: " + instructorId);
      verifyNoInteractions(instructorRepository);
      verify(courseRepository, never()).save(any(CourseEntity.class));
    }

    @Test
    @DisplayName("throws 404 when the instructor does not exist")
    void throwsNotFoundWhenInstructorDoesNotExist() {
      // GIVEN
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.of(buildCourseEntity(COURSE)));
      when(courseRepository.existsByNameAndIdNot(UPDATED_COURSE.name(), COURSE.id())).thenReturn(false);
      when(instructorRepository.findById(UPDATED_COURSE.instructor().id())).thenReturn(Optional.empty());

      // WHEN / THEN
      assertFailsWith(
              "unknown instructor",
              () -> courseService.update(COURSE.id().toString(), buildUpdateCourseRequest(UPDATED_COURSE)),
              HttpStatus.NOT_FOUND,
              "Instructor not found: " + UPDATED_COURSE.instructor().id());
      verify(courseRepository, never()).save(any(CourseEntity.class));
    }

    @ParameterizedTest
    @MethodSource("com.teletubbies.course.course.CourseServiceTest#provideUnparsableCourseIds")
    @DisplayName("throws 404 when the id is not a valid UUID")
    void throwsNotFoundWhenIdIsNotAUuid(final String testCase, final String courseId) {
      // WHEN / THEN
      assertFailsWith(
              testCase,
              () -> courseService.update(courseId, buildUpdateCourseRequest(UPDATED_COURSE)),
              HttpStatus.NOT_FOUND,
              "Course not found: " + courseId);
      verifyNoInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @Test
    @DisplayName("throws 404 when the course does not exist")
    void throwsNotFoundWhenCourseDoesNotExist() {
      // GIVEN
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.empty());

      // WHEN / THEN
      assertFailsWith(
              "existing id, missing row",
              () -> courseService.update(COURSE.id().toString(), buildUpdateCourseRequest(UPDATED_COURSE)),
              HttpStatus.NOT_FOUND,
              "Course not found: " + COURSE.id());
      verify(courseRepository, never()).save(any(CourseEntity.class));
      verifyNoInteractions(instructorRepository);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // delete(String)
  // ---------------------------------------------------------------------------------------------

  @Nested
  @DisplayName("delete(String)")
  class Delete {

    @Test
    @DisplayName("deletes the existing course")
    void deletesTheExistingCourse() {
      // GIVEN
      final CourseEntity course = buildCourseEntity(COURSE);
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.of(course));

      // WHEN
      courseService.delete(COURSE.id().toString());

      // THEN
      verify(courseRepository).findById(COURSE.id());
      verify(courseRepository).delete(course);
      verifyNoMoreInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @ParameterizedTest
    @MethodSource("com.teletubbies.course.course.CourseServiceTest#provideUnparsableCourseIds")
    @DisplayName("throws 404 when the id is not a valid UUID")
    void throwsNotFoundWhenIdIsNotAUuid(final String testCase, final String courseId) {
      // WHEN / THEN
      assertFailsWith(
              testCase,
              () -> courseService.delete(courseId),
              HttpStatus.NOT_FOUND,
              "Course not found: " + courseId);
      verifyNoInteractions(courseRepository);
      verifyNoInteractions(instructorRepository);
    }

    @Test
    @DisplayName("throws 404 when the course does not exist")
    void throwsNotFoundWhenCourseDoesNotExist() {
      // GIVEN
      when(courseRepository.findById(COURSE.id())).thenReturn(Optional.empty());

      // WHEN / THEN
      assertFailsWith(
              "existing id, missing row",
              () -> courseService.delete(COURSE.id().toString()),
              HttpStatus.NOT_FOUND,
              "Course not found: " + COURSE.id());
      verify(courseRepository).findById(COURSE.id());
      verify(courseRepository, never()).delete(any(CourseEntity.class));
      verifyNoMoreInteractions(courseRepository);
    }
  }

  // ---------------------------------------------------------------------------------------------
  // Shared parameterized test data. Declared here (not inside a @Nested class) because
  // @MethodSource on a @Nested test needs a fully qualified "ClassName#methodName" reference
  // to reach a static method on the enclosing class.
  // ---------------------------------------------------------------------------------------------

  static Stream<Arguments> provideUnparsableCourseIds() {
    return Stream.of(
            Arguments.of("Empty id", ""),
            Arguments.of("Blank id", " "),
            Arguments.of("Not a uuid", "not-a-uuid"),
            Arguments.of("No separators", "12345"),
            Arguments.of("Missing the last group", "00000000-0000-0000-0000"),
            Arguments.of("Too long uuid", "00000000-0000-0000-0000-000000000101-102"),
            Arguments.of("Non hexadecimal character", "00000000-0000-0000-0000-00000000010Z"));
  }

  static Stream<Arguments> provideInvalidInstructorIds() {
    return Stream.of(
            Arguments.of("Empty instructorId", ""),
            Arguments.of("Blank instructorId", " "),
            Arguments.of("Not a uuid", "not-a-uuid"),
            Arguments.of("No separators", "12345"),
            Arguments.of("Missing the last group", "00000000-0000-0000-0000"),
            Arguments.of("Non hexadecimal character", "00000000-0000-0000-0000-00000000010Z"));
  }

  // ---------------------------------------------------------------------------------------------
  // Assertion helper. Every "invalid input" test in this file ends up checking the same three
  // things on the thrown exception (type, message, HTTP status) — pulling that into one method
  // turns a 6-line block into a single call and keeps the three checks consistent everywhere.
  // ---------------------------------------------------------------------------------------------

  private static void assertFailsWith(
          final ThrowingCallable callable, final HttpStatus expectedStatus, final String expectedMessage) {
    assertThatThrownBy(callable)
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining(expectedMessage)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(expectedStatus);
  }

  /** Same as {@link #assertFailsWith(ThrowingCallable, HttpStatus, String)}, with an AssertJ
   * description — use this one inside parameterized tests so a failure names which case broke. */
  private static void assertFailsWith(
          final String description,
          final ThrowingCallable callable,
          final HttpStatus expectedStatus,
          final String expectedMessage) {
    assertThatThrownBy(callable)
            .as(description)
            .isInstanceOf(ResponseStatusException.class)
            .hasMessageContaining(expectedMessage)
            .extracting(e -> ((ResponseStatusException) e).getStatusCode())
            .isEqualTo(expectedStatus);
  }

  // ---------------------------------------------------------------------------------------------
  // Fixture -> entity / request / resource builders
  // ---------------------------------------------------------------------------------------------

  /** The database generates the id, so every fixture has to carry it before being mapped. */
  private static InstructorEntity buildInstructorEntity(final InstructorFixture fixture) {
    final InstructorEntity instructor =
            new InstructorEntity(fixture.fullName(), fixture.email(), "hash", fixture.role());
    ReflectionTestUtils.setField(instructor, "id", fixture.id());
    return instructor;
  }

  private static CourseEntity buildCourseEntity(final CourseFixture fixture) {
    final CourseEntity course =
            new CourseEntity(buildNewCourseRequest(fixture), buildInstructorEntity(fixture.instructor()));
    ReflectionTestUtils.setField(course, "id", fixture.id());
    return course;
  }

  private static CourseFixture copyWithId(final UUID id) {
    return new CourseFixture(
            id,
            CourseServiceTest.COURSE.name(),
            CourseServiceTest.COURSE.description(),
            CourseServiceTest.COURSE.duration(),
            CourseServiceTest.COURSE.level(),
            CourseServiceTest.COURSE.category(),
            CourseServiceTest.COURSE.instructor());
  }

  private static NewCourseRequest buildNewCourseRequest(final CourseFixture fixture) {
    return buildNewCourseRequest(fixture, fixture.instructor().id().toString());
  }

  private static NewCourseRequest buildNewCourseRequest(
          final CourseFixture fixture, final String instructorId) {
    return NewCourseRequest.builder()
            .name(fixture.name())
            .description(fixture.description())
            .duration(fixture.duration())
            .level(fixture.level())
            .category(fixture.category())
            .instructorId(instructorId)
            .build();
  }

  private static UpdateCourseRequest buildUpdateCourseRequest(final CourseFixture fixture) {
    return buildUpdateCourseRequest(fixture, fixture.instructor().id().toString());
  }

  private static UpdateCourseRequest buildUpdateCourseRequest(
          final CourseFixture fixture, final String instructorId) {
    return UpdateCourseRequest.builder()
            .name(fixture.name())
            .description(fixture.description())
            .duration(fixture.duration())
            .level(fixture.level())
            .category(fixture.category())
            .instructorId(instructorId)
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

  private static CourseResource buildCourseResource(final CourseFixture fixture) {
    return CourseResource.builder()
            .id(fixture.id().toString())
            .name(fixture.name())
            .description(fixture.description())
            .duration(fixture.duration())
            .level(fixture.level())
            .category(fixture.category())
            .instructor(buildInstructorResource(fixture.instructor()))
            .build();
  }
}


