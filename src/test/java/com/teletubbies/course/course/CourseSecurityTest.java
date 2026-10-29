package com.teletubbies.course.course;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class CourseSecurityTest {

  private static final String COURSE_ID = "00000000-0000-0000-0000-000000000101";
  private static final String OWNER_EMAIL = "laalaa@teletubbies.test";

  @Mock private CourseRepository courseRepository;

  @InjectMocks private CourseSecurity courseSecurity;

  static Stream<Arguments> isOwner_data_provider() {
    return Stream.of(
        Arguments.of("Instructor owns the course", true),
        Arguments.of("Instructor does not own the course", false));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("isOwner_data_provider")
  void isOwner_should_return_what_the_repository_answers(
      final String testCase, final boolean owns) {
    // GIVEN isOwner_data_provider
    when(courseRepository.existsByIdAndInstructor_Email(UUID.fromString(COURSE_ID), OWNER_EMAIL))
        .thenReturn(owns);

    // WHEN
    final boolean result = courseSecurity.isOwner(COURSE_ID, authentication());

    // THEN
    assertThat(result).as(testCase).isEqualTo(owns);
  }

  static Stream<Arguments> isOwner_invalid_id_data_provider() {
    return Stream.of(
        Arguments.of("Empty id", ""),
        Arguments.of("Blank id", " "),
        Arguments.of("Not a UUID", "not-a-uuid"),
        Arguments.of("Non-hex characters", "zzzzzzzz-0000-0000-0000-000000000101"));
  }

  @ParameterizedTest(name = "{0}")
  @MethodSource("isOwner_invalid_id_data_provider")
  void isOwner_should_return_false_without_querying_when_id_is_not_a_uuid(
      final String testCase, final String courseId) {
    // GIVEN isOwner_invalid_id_data_provider

    // WHEN
    final boolean result = courseSecurity.isOwner(courseId, authentication());

    // THEN
    assertThat(result).as(testCase).isFalse();
    verifyNoInteractions(courseRepository);
  }

  private Authentication authentication() {
    return new TestingAuthenticationToken(OWNER_EMAIL, null, "ROLE_TEACHER");
  }
}
