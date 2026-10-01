package com.teletubbies.course.instructor;

import org.springframework.web.bind.annotation.RestController;

import com.teletubbies.course.InstructorsApi;
import com.teletubbies.course.model.InstructorResource;
import com.teletubbies.course.model.InstructorResponse;
import com.teletubbies.course.model.InstructorsData;
import com.teletubbies.course.model.InstructorsPagedResources;
import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.PageResource;
import com.teletubbies.course.model.UpdateInstructorRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;

@RestController
public class InstructorController implements InstructorsApi {

  private final InstructorService instructorService;

  public InstructorController(final InstructorService instructorService) {
    this.instructorService = instructorService;
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ResponseEntity<InstructorResponse> createInstructor(
      final NewInstructorRequest newInstructorRequest) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(
            InstructorResponse.builder()
                .instructor(instructorService.create(newInstructorRequest))
                .build());
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'TEACHER')")
  public ResponseEntity<InstructorsPagedResources> getAllInstructors(
      final Pageable pageable,
      final String fullName,
      final String email,
      final InstructorRoleType role) {
    final Page<InstructorResource> page =
        instructorService.getAll(fullName, email, role, pageable);

    return ResponseEntity.ok(
        InstructorsPagedResources.builder()
            .data(
                InstructorsData.builder()
                    .content(page.getContent())
                    .size(page.getNumberOfElements())
                    .build())
            .page(PageResource.builder().number(page.getNumber()).size(page.getSize()).build())
            .totalElements(page.getTotalElements())
            .build());
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'TEACHER')")
  public ResponseEntity<InstructorResponse> getInstructor(final String instructorId) {
    return ResponseEntity.ok(
        InstructorResponse.builder().instructor(instructorService.getById(instructorId)).build());
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ResponseEntity<InstructorResponse> updateInstructor(
      final String instructorId, final UpdateInstructorRequest updateInstructorRequest) {
    return ResponseEntity.ok(
        InstructorResponse.builder()
            .instructor(instructorService.update(instructorId, updateInstructorRequest))
            .build());
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ResponseEntity<Void> deleteInstructor(final String instructorId) {
    instructorService.delete(instructorId);
    return ResponseEntity.noContent().build();
  }
}