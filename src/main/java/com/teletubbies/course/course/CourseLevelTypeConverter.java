package com.teletubbies.course.course;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class CourseLevelTypeConverter implements AttributeConverter<CourseLevelType, Short> {

  @Override
  public Short convertToDatabaseColumn(final CourseLevelType attribute) {
    return Optional.ofNullable(attribute).map(CourseLevelType::getKey).orElse(null);
  }

  @Override
  public CourseLevelType convertToEntityAttribute(final Short dbData) {
    return Optional.ofNullable(dbData).map(CourseLevelType::fromKey).orElse(null);
  }
}
