package com.teletubbies.course.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.teletubbies.course.BaseRepositoryTest;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;

class CourseRepositoryTest extends BaseRepositoryTest {
  private static final String DBUNIT_XML = "classpath:dbunit/repository/courses.xml";

  @Autowired private CourseRepository courseRepository;

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAll_should_return_every_seeded_course() {
    // GIVEN courses.xml

    // WHEN
    final List<CourseEntity> courses = courseRepository.findAll();

    // THEN
    assertThat(courses)
        .extracting(CourseEntity::getName)
        .containsExactlyInAnyOrder("Introduction to Spring Boot", "UI Design with Figma");
  }

  static Stream<Arguments> findById_data_provider() {
    return Stream.of(
        Arguments.of(
            "Existing course id", UUID.fromString("00000000-0000-0000-0000-000000000101"), true),
        Arguments.of(
            "Non-existing course id",
            UUID.fromString("00000000-0000-0000-0000-000000000999"),
            false));
  }

  @DatabaseSetup(DBUNIT_XML)
  @ParameterizedTest
  @MethodSource("findById_data_provider")
  void findById_should_return_expected_results(
      final String testCase, final UUID courseId, final boolean expectedPresent) {
    // GIVEN findById_data_provider

    // WHEN
    final Optional<CourseEntity> result = courseRepository.findById(courseId);

    // THEN
    assertThat(result.isPresent()).as(testCase).isEqualTo(expectedPresent);
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findById_should_apply_enum_converters_and_map_all_fields() {
    // GIVEN courses.xml

    // WHEN
    final CourseEntity course =
        courseRepository
            .findById(UUID.fromString("00000000-0000-0000-0000-000000000101"))
            .orElseThrow();

    // THEN
    assertThat(course.getName()).isEqualTo("Introduction to Spring Boot");
    assertThat(course.getDescription())
        .isEqualTo("Learn the fundamentals of building REST APIs with Spring Boot.");
    assertThat(course.getDuration()).isEqualTo((short) 12);
    assertThat(course.getLevel()).isEqualTo(CourseLevelType.BEGINNER);
    assertThat(course.getCategory()).isEqualTo(CourseCategoryType.PROGRAMMING);
    assertThat(course.getInstructorId())
        .isEqualTo(UUID.fromString("00000000-0000-0000-0000-000000000001"));
  }

  static Stream<Arguments> existsByInstructorId_data_provider() {
    return Stream.of(
        Arguments.of(
            "Instructor with courses", UUID.fromString("00000000-0000-0000-0000-000000000001"), true),
        Arguments.of(
            "Instructor without courses",
            UUID.fromString("00000000-0000-0000-0000-000000000999"),
            false));
  }

  @DatabaseSetup(DBUNIT_XML)
  @ParameterizedTest
  @MethodSource("existsByInstructorId_data_provider")
  void existsByInstructorId_should_return_expected_results(
      final String testCase, final UUID instructorId, final boolean expectedResult) {
    // GIVEN existsByInstructorId_data_provider

    // WHEN
    final boolean result = courseRepository.existsByInstructorId(instructorId);

    // THEN
    assertThat(result).as(testCase).isEqualTo(expectedResult);
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void existsByNameAndIdNot_should_ignore_the_course_itself() {
    // GIVEN courses.xml
    final UUID ownId = UUID.fromString("00000000-0000-0000-0000-000000000101");

    // WHEN
    final boolean ownName = courseRepository.existsByNameAndIdNot("Introduction to Spring Boot", ownId);
    final boolean otherName = courseRepository.existsByNameAndIdNot("UI Design with Figma", ownId);

    // THEN
    assertThat(ownName).isFalse();
    assertThat(otherName).isTrue();
  }
}
