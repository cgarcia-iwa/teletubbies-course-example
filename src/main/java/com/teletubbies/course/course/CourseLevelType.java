package com.teletubbies.course.course;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enumerates the difficulty levels available for a course.
 *
 * <p>Replaces the type generated from {@code enum.yaml#/components/schemas/CourseLevel} (see the
 * {@code schemaMappings} entry in {@code pom.xml}) so the numeric {@code key} persisted in the
 * {@code course.level} column can live next to the value it represents, instead of a separate,
 * ordinal-position-dependent mapping.
 */
@Getter
@RequiredArgsConstructor
public enum CourseLevelType {
  BEGINNER((short) 1),
  INTERMEDIATE((short) 2),
  ADVANCED((short) 3);

  private final Short key;

  @JsonValue
  public String getValue() {
    return name();
  }

  @JsonCreator
  public static CourseLevelType fromValue(final String value) {
    return Arrays.stream(CourseLevelType.values())
        .filter(level -> level.name().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
  }

  public static CourseLevelType fromKey(final Short key) {
    return Arrays.stream(CourseLevelType.values())
        .filter(level -> level.key.equals(key))
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unexpected course level key '" + key + "'"));
  }
}
