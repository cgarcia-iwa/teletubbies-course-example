package com.teletubbies.course.course;

import java.util.List;
import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

@Repository
@Validated
public interface CourseRepository
    extends JpaRepository<CourseEntity, UUID>, JpaSpecificationExecutor<CourseEntity> {
  boolean existsByName(String name);

  boolean existsByNameAndIdNot(String name, UUID id);

  boolean existsByInstructorId(UUID instructorId);

  /**
   * Equivalent to this implicit JPQL:
   *
   * <pre>
   * SELECT CASE WHEN COUNT(c) > 0 THEN true ELSE false END
   * FROM CourseEntity c
   * JOIN c.instructor i
   * WHERE c.id = :id AND i.email = :email
   * </pre>
   *
   * In other words: does a course with that id exist whose associated instructor has that
   * email? That's why {@code CourseSecurity.isOwner} uses it in a single query: it compares the
   * course from the path against the authenticated user's email ({@code
   * authentication.getName()}), without needing to load the full {@code InstructorEntity} or
   * {@code CourseEntity} like the previous version did with two repositories.
   */
  boolean existsByIdAndInstructor_Email(@NotNull UUID id, @NotBlank String email);

  @Override
  @EntityGraph(attributePaths = "instructor")
  List<CourseEntity> findAll();

  @Override
  @EntityGraph(attributePaths = "instructor")
  Page<CourseEntity> findAll(Specification<CourseEntity> spec, Pageable pageable);
}
