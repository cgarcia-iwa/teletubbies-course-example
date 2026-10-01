package com.teletubbies.course.course;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.teletubbies.course.BaseRepositoryTest;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class CourseSpecificationTest extends BaseRepositoryTest {

  private static final String DBUNIT_XML = "classpath:dbunit/repository/courses.xml";

  @Autowired private CourseRepository courseRepository;

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_return_every_course_when_no_filter_is_given() {
    // GIVEN courses.xml has 3 courses

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(CourseSpecification.findAllWithFilters(null, null, null, null));

    // THEN
    assertThat(result).hasSize(3);
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_name_by_partial_case_insensitive_match() {
    // GIVEN "spring" matches "Introduction to Spring Boot" regardless of case

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(
            CourseSpecification.findAllWithFilters("spring", null, null, null));

    // THEN
    assertThat(result)
        .extracting(CourseEntity::getName)
        .containsExactly("Introduction to Spring Boot");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_by_level() {
    // GIVEN only "Advanced Data Science" is ADVANCED

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(
            CourseSpecification.findAllWithFilters(null, CourseLevelType.ADVANCED, null, null));

    // THEN
    assertThat(result)
        .extracting(CourseEntity::getName)
        .containsExactly("Advanced Data Science");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_by_category() {
    // GIVEN only "UI Design with Figma" belongs to DESIGN

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(
            CourseSpecification.findAllWithFilters(
                null, null, CourseCategoryType.DESIGN, null));

    // THEN
    assertThat(result)
        .extracting(CourseEntity::getName)
        .containsExactly("UI Design with Figma");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_by_instructorId() {
    // GIVEN instructor 2 (Po-Po) only teaches "Advanced Data Science"
    final UUID poPoId = UUID.fromString("00000000-0000-0000-0000-000000000002");

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(CourseSpecification.findAllWithFilters(null, null, null, poPoId));

    // THEN
    assertThat(result)
        .extracting(CourseEntity::getName)
        .containsExactly("Advanced Data Science");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_combine_every_filter() {
    // GIVEN only "Introduction to Spring Boot" matches all of: name, level, category and
    // instructor at the same time
    final UUID laaLaaId = UUID.fromString("00000000-0000-0000-0000-000000000001");

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(
            CourseSpecification.findAllWithFilters(
                "Introduction", CourseLevelType.BEGINNER, CourseCategoryType.PROGRAMMING, laaLaaId));

    // THEN
    assertThat(result)
        .extracting(CourseEntity::getName)
        .containsExactly("Introduction to Spring Boot");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_return_empty_when_no_course_matches() {
    // GIVEN no course is named "nonexistent"

    // WHEN
    final List<CourseEntity> result =
        courseRepository.findAll(
            CourseSpecification.findAllWithFilters("nonexistent", null, null, null));

    // THEN
    assertThat(result).isEmpty();
  }
}
