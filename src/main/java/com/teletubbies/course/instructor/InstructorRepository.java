package com.teletubbies.course.instructor;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.validation.annotation.Validated;

@Repository
@Validated
public interface InstructorRepository extends JpaRepository<InstructorEntity, UUID> {
  boolean existsByFullName(String fullName);

  boolean existsByEmail(String email);

  boolean existsByFullNameAndIdNot(String fullName, UUID id);

  boolean existsByEmailAndIdNot(String email, UUID id);
}
