package com.teletubbies.course.exception;

/**
 * A rejected field of the request, serialized inside the {@code errors} property of the
 * ProblemDetail.
 *
 * @param field name of the rejected field
 * @param message reason the field was rejected
 */
public record FieldViolation(String field, String message) {}
