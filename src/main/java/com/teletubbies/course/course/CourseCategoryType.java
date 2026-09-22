package com.teletubbies.course.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerates the categories available for a course.
 *
 * <p>Replaces the type generated from {@code enum.yaml#/components/schemas/CourseCategory} (see
 * the {@code schemaMappings} entry in {@code pom.xml}) so the numeric {@code key} persisted in
 * the {@code course.category} column can live next to the value it represents, instead of a
 * separate, ordinal-position-dependent mapping.
 */
@Getter
@RequiredArgsConstructor
public enum CourseCategoryType {
  PROGRAMMING((short) 1),
  DESIGN((short) 2),
  BUSINESS((short) 3),
  LANGUAGES((short) 4),
  SCIENCE((short) 5),
  ARTS((short) 6);

  private final Short key;

  @JsonValue
  public String getValue() {
    return name();
  }

  @JsonCreator
  public static CourseCategoryType fromValue(final String value) {
    return Arrays.stream(CourseCategoryType.values())
        .filter(category -> category.name().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
  }

  public static CourseCategoryType fromKey(final Short key) {
    return Arrays.stream(CourseCategoryType.values())
        .filter(category -> category.key.equals(key))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unexpected course category key '" + key + "'"));
  }
}
