package com.teletubbies.course.instructor;

import com.teletubbies.course.course.CourseRepository;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class InstructorService {

  private final InstructorRepository instructorRepository;
  private final CourseRepository courseRepository;

  public InstructorService(
      final InstructorRepository instructorRepository, final CourseRepository courseRepository) {
    this.instructorRepository = instructorRepository;
    this.courseRepository = courseRepository;
  }

  @Transactional(readOnly = true)
  public List<InstructorResource> getAll() {
    return instructorRepository.findAll().stream().map(this::toResource).toList();
  }

  @Transactional(readOnly = true)
  public InstructorResource getById(final String instructorId) {
    return toResource(findByIdOrThrow(instructorId));
  }

  @Transactional
  public InstructorResource create(final NewInstructorRequest request) {
    if (instructorRepository.existsByFullName(request.getFullName())) {
      throw conflict("Instructor full name already exists: " + request.getFullName());
    }
    if (instructorRepository.existsByEmail(request.getEmail())) {
      throw conflict("Instructor email already exists: " + request.getEmail());
    }
    return toResource(instructorRepository.save(new InstructorEntity(request)));
  }

  @Transactional
  public InstructorResource update(final String instructorId, final UpdateInstructorRequest request) {
    final InstructorEntity instructor = findByIdOrThrow(instructorId);
    if (instructorRepository.existsByFullNameAndIdNot(request.getFullName(), instructor.getId())) {
      throw conflict("Instructor full name already exists: " + request.getFullName());
    }
    if (instructorRepository.existsByEmailAndIdNot(request.getEmail(), instructor.getId())) {
      throw conflict("Instructor email already exists: " + request.getEmail());
    }
    instructor.update(request);
    return toResource(instructorRepository.save(instructor));
  }

  @Transactional
  public void delete(final String instructorId) {
    final InstructorEntity instructor = findByIdOrThrow(instructorId);
    if (courseRepository.existsByInstructorId(instructor.getId())) {
      throw conflict("Instructor has assigned courses: " + instructorId);
    }
    instructorRepository.delete(instructor);
  }

  private InstructorEntity findByIdOrThrow(final String instructorId) {
    final UUID id;
    try {
      id = UUID.fromString(instructorId);
    } catch (final IllegalArgumentException e) {
      throw notFound(instructorId);
    }
    return instructorRepository.findById(id).orElseThrow(() -> notFound(instructorId));
  }

  private ResponseStatusException notFound(final String instructorId) {
    return new ResponseStatusException(HttpStatus.NOT_FOUND, "Instructor not found: " + instructorId);
  }

  private ResponseStatusException conflict(final String message) {
    return new ResponseStatusException(HttpStatus.CONFLICT, message);
  }

  private InstructorResource toResource(final InstructorEntity instructor) {
    return InstructorResource.builder()
        .id(instructor.getId().toString())
        .fullName(instructor.getFullName())
        .email(instructor.getEmail())
        .build();
  }
}
