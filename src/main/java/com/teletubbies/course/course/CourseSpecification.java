package com.teletubbies.course.course;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.jpa.domain.Specification;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class CourseSpecification {

  public static Specification<CourseEntity> findAllWithFilters(
      final String name,
      final CourseLevelType level,
      final CourseCategoryType category,
      final UUID instructorId) {
    return (root, query, cb) -> {
      final List<Predicate> predicates = new ArrayList<>();

      if (StringUtils.isNotBlank(name)) {
        predicates.add(cb.like(cb.lower(root.get(CourseEntity_.name)), "%" + name.toLowerCase() + "%"));
      }
      if (level != null) {
        predicates.add(cb.equal(root.get(CourseEntity_.level), level));
      }
      if (category != null) {
        predicates.add(cb.equal(root.get(CourseEntity_.category), category));
      }
      if (instructorId != null) {
        predicates.add(cb.equal(root.get(CourseEntity_.instructorId), instructorId));
      }

      return cb.and(predicates.toArray(Predicate[]::new));
    };
  }
}
