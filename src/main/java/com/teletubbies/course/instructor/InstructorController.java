package com.teletubbies.course.instructor;

import com.teletubbies.course.InstructorsApi;
import com.teletubbies.course.model.InstructorResponse;
import com.teletubbies.course.model.InstructorsResponse;
import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class InstructorController implements InstructorsApi {

  private final InstructorService instructorService;

  public InstructorController(final InstructorService instructorService) {
    this.instructorService = instructorService;
  }

  @Override
  public ResponseEntity<InstructorResponse> createInstructor(
      final NewInstructorRequest newInstructorRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            InstructorResponse.builder()
                .instructor(instructorService.create(newInstructorRequest))
                .build());
  }

  @Override
  public ResponseEntity<InstructorsResponse> getAllInstructors() {
    return ResponseEntity.ok(
        InstructorsResponse.builder().instructors(instructorService.getAll()).build());
  }

  @Override
  public ResponseEntity<InstructorResponse> getInstructor(final String instructorId) {
    return ResponseEntity.ok(
        InstructorResponse.builder().instructor(instructorService.getById(instructorId)).build());
  }

  @Override
  public ResponseEntity<InstructorResponse> updateInstructor(
      final String instructorId, final UpdateInstructorRequest updateInstructorRequest) {
    return ResponseEntity.ok(
        InstructorResponse.builder()
            .instructor(instructorService.update(instructorId, updateInstructorRequest))
            .build());
  }

  @Override
  public ResponseEntity<Void> deleteInstructor(final String instructorId) {
    instructorService.delete(instructorId);
    return ResponseEntity.noContent().build();
  }
}
