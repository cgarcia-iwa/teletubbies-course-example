package com.teletubbies.course.course;

import com.teletubbies.course.instructor.InstructorEntity;
import com.teletubbies.course.instructor.InstructorRepository;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Slf4j
@Service
public class CourseService {

  private final CourseRepository courseRepository;
  private final InstructorRepository instructorRepository;

  public CourseService(
      final CourseRepository courseRepository, final InstructorRepository instructorRepository) {
    this.courseRepository = courseRepository;
    this.instructorRepository = instructorRepository;
  }

  @Transactional(readOnly = true)
  public List<CourseResource> getAll() {
    return courseRepository.findAll().stream().map(this::toResource).toList();
  }

  @Transactional(readOnly = true)
  public CourseResource getById(final String courseId) {
    return toResource(findByIdOrThrow(courseId));
  }

  @Transactional
  public CourseResource create(final NewCourseRequest request) {
    if (courseRepository.existsByName(request.getName())) {
      throw conflict("Course name already exists: " + request.getName());
    }
    final InstructorEntity instructor = findInstructorOrThrow(request.getInstructorId());
    return toResource(courseRepository.save(new CourseEntity(request, instructor)));
  }

  @Transactional
  public CourseResource update(final String courseId, final UpdateCourseRequest request) {
    final CourseEntity course = findByIdOrThrow(courseId);
    if (courseRepository.existsByNameAndIdNot(request.getName(), course.getId())) {
      throw conflict("Course name already exists: " + request.getName());
    }
    course.update(request, findInstructorOrThrow(request.getInstructorId()));
    return toResource(courseRepository.save(course));
  }

  @Transactional
  public void delete(final String courseId) {
    courseRepository.delete(findByIdOrThrow(courseId));
  }

  /**
   * Resolves the instructor referenced by the request. The course carries it as an association
   * because that is what feeds the response, so a reference that cannot be resolved is rejected
   * instead of leaving the course without an instructor.
   */
  private InstructorEntity findInstructorOrThrow(final String instructorId) {
    final UUID id;
    try {
      id = UUID.fromString(instructorId);
    } catch (final IllegalArgumentException e) {
      throw new ResponseStatusException(
          HttpStatus.BAD_REQUEST, "Invalid instructorId: " + instructorId);
    }
    return instructorRepository
        .findById(id)
        .orElseThrow(
            () ->
                new ResponseStatusException(
                    HttpStatus.NOT_FOUND, "Instructor not found: " + instructorId));
  }

  private CourseEntity findByIdOrThrow(final String courseId) {
    final UUID id;
    try {
      id = UUID.fromString(courseId);
    } catch (final IllegalArgumentException e) {
      throw notFound(courseId);
    }
    return courseRepository.findById(id).orElseThrow(() -> notFound(courseId));
  }

  private ResponseStatusException notFound(final String courseId) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + courseId);
  }

  private ResponseStatusException conflict(final String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private CourseResource toResource(final CourseEntity course) {
    return CourseResource.builder()
        .id(course.getId().toString())
        .name(course.getName())
        .description(course.getDescription())
        .duration(course.getDuration().intValue())
        .level(course.getLevel())
        .category(course.getCategory())
        .instructor(toResource(course.getInstructor()))
        .build();
  }

  private InstructorResource toResource(final InstructorEntity instructor) {
    return InstructorResource.builder()
        .id(instructor.getId().toString())
        .fullName(instructor.getFullName())
        .email(instructor.getEmail())
        .role(instructor.getRole())
        .build();
  }
}
