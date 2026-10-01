package com.teletubbies.course.instructor;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class InstructorSpecification {

  public static Specification<InstructorEntity> findAllWithFilters(
      final String fullName, final String email, final InstructorRoleType role) {
    return (root, query, cb) -> {
      final List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.isNotBlank(fullName)) {
        predicates.add(
            cb.like(cb.lower(root.get(InstructorEntity_.fullName)), "%" + fullName.toLowerCase() + "%"));
      }
      if (StringUtils.isNotBlank(email)) {
        predicates.add(
            cb.like(cb.lower(root.get(InstructorEntity_.email)), "%" + email.toLowerCase() + "%"));
      }
      if (role != null) {
        predicates.add(cb.equal(root.get(InstructorEntity_.role), role));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
