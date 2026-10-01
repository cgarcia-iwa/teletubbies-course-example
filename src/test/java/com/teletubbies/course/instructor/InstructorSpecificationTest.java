package com.teletubbies.course.instructor;

import static org.assertj.core.api.Assertions.assertThat;

import com.github.springtestdbunit.annotation.DatabaseSetup;
import com.teletubbies.course.BaseRepositoryTest;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class InstructorSpecificationTest extends BaseRepositoryTest {

  private static final String DBUNIT_XML = "classpath:dbunit/repository/courses.xml";

  @Autowired private InstructorRepository instructorRepository;

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_return_every_instructor_when_no_filter_is_given() {
    // GIVEN courses.xml has 2 instructors

    // WHEN
    final List<InstructorEntity> result =
        instructorRepository.findAll(InstructorSpecification.findAllWithFilters(null, null, null));

    // THEN
    assertThat(result).hasSize(2);
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_fullName_by_partial_case_insensitive_match() {
    // GIVEN "laa" matches "Laa-Laa" regardless of case

    // WHEN
    final List<InstructorEntity> result =
        instructorRepository.findAll(
            InstructorSpecification.findAllWithFilters("laa", null, null));

    // THEN
    assertThat(result).extracting(InstructorEntity::getFullName).containsExactly("Laa-Laa");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_email_by_partial_case_insensitive_match() {
    // GIVEN only Po-Po's email starts with "popo"

    // WHEN
    final List<InstructorEntity> result =
        instructorRepository.findAll(
            InstructorSpecification.findAllWithFilters(null, "POPO", null));

    // THEN
    assertThat(result).extracting(InstructorEntity::getEmail).containsExactly("popo@teletubbies.test");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_filter_by_role() {
    // GIVEN only Po-Po has the ADMINISTRATOR role

    // WHEN
    final List<InstructorEntity> result =
        instructorRepository.findAll(
            InstructorSpecification.findAllWithFilters(null, null, InstructorRoleType.ADMINISTRATOR));

    // THEN
    assertThat(result).extracting(InstructorEntity::getFullName).containsExactly("Po-Po");
  }

  @DatabaseSetup(DBUNIT_XML)
  @Test
  void findAllWithFilters_should_return_empty_when_no_instructor_matches() {
    // GIVEN no instructor is named "nonexistent"

    // WHEN
    final List<InstructorEntity> result =
        instructorRepository.findAll(
            InstructorSpecification.findAllWithFilters("nonexistent", null, null));

    // THEN
    assertThat(result).isEmpty();
  }
}
