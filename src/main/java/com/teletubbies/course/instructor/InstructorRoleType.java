package com.teletubbies.course.instructor;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.Arrays;
import lombok.Getter;

/**
 * Enumerates the roles an instructor can have.
 *
 * <p>Replaces the type generated from {@code enum.yaml#/components/schemas/InstructorRole} (see
 * the {@code schemaMappings} entry in {@code pom.xml}) so the numeric {@code key} persisted in the
 * {@code instructor.role} column can live next to the value it represents.
 */
@Getter
public enum InstructorRoleType {
  ADMINISTRATOR(1),
  TEACHER(2);

  private final short key;

  InstructorRoleType(final int key) {
    this.key = (short) key;
  }

  public static InstructorRoleType fromValue(final String value) {
    return Arrays.stream(InstructorRoleType.values())
        .filter(role -> role.name().equals(value))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Unexpected value '" + value + "'"));
  }

  public static InstructorRoleType fromKey(final short key) {
    return Arrays.stream(InstructorRoleType.values())
        .filter(role -> role.key == key)
        .findFirst()
        .orElseThrow(
            () -> new IllegalArgumentException("Unexpected instructor role key '" + key + "'"));
  }
}