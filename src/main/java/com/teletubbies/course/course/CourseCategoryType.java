package com.teletubbies.course.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

/**
 * Enumerates the categories available for a course.
 *
 * <p>Replaces the type generated from {@code enum.yaml#/components/schemas/CourseCategory} (see
 * the {@code schemaMappings} entry in {@code pom.xml}) so the numeric {@code key} persisted in
 * the {@code course.category} column can live next to the value it represents, instead of a
 * separate, ordinal-position-dependent mapping.
 */
@Getter
public enum CourseCategoryType {
  PROGRAMMING(1),
  DESIGN(2),
  BUSINESS(3),
  LANGUAGES(4),
  SCIENCE(5),
  ARTS(6);

  private final Short key;

  CourseCategoryType(final int key) {
    this.key = (short) key;
  }

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
