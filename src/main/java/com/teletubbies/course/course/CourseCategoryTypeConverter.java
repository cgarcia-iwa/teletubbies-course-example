package com.teletubbies.course.course;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;
import java.util.Optional;

@Converter(autoApply = true)
public class CourseCategoryTypeConverter
    implements AttributeConverter<CourseCategoryType, Integer> {

  @Override
  public Integer convertToDatabaseColumn(final CourseCategoryType attribute) {
    return Optional.ofNullable(attribute).map(CourseCategoryType::getKey).orElse(null);
  }

  @Override
  public CourseCategoryType convertToEntityAttribute(final Integer dbData) {
    return Optional.ofNullable(dbData).map(CourseCategoryType::fromKey).orElse(null);
  }
}
