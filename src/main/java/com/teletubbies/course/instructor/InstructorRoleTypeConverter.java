package com.teletubbies.course.instructor;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class InstructorRoleTypeConverter implements AttributeConverter<InstructorRoleType, Short> {

  @Override
  public Short convertToDatabaseColumn(final InstructorRoleType attribute) {
    return Optional.ofNullable(attribute).map(InstructorRoleType::getKey).orElse(null);
  }

  @Override
  public InstructorRoleType convertToEntityAttribute(final Short dbData) {
    return Optional.ofNullable(dbData).map(InstructorRoleType::fromKey).orElse(null);
  }
}