package com.teletubbies.course.course;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

@Repository
@Validated
public interface CourseRepository extends JpaRepository<CourseEntity, UUID> {
  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, UUID id);

  boolean existsByInstructorId(UUID instructorId);

  @Override
  @EntityGraph(attributePaths = "instructor")
  List<CourseEntity> findAll();
}
