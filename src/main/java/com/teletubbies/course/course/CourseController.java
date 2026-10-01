package com.teletubbies.course.course;

import com.teletubbies.course.CoursesApi;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.CourseResponse;
import com.teletubbies.course.model.CoursesData;
import com.teletubbies.course.model.CoursesPagedResources;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.PageResource;
import com.teletubbies.course.model.UpdateCourseRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseController implements CoursesApi {

  private final CourseService courseService;

  public CourseController(final CourseService courseService) {
    this.courseService = courseService;
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ResponseEntity<CourseResponse> createCourse(final NewCourseRequest newCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
            CourseResponse.builder()
                    .course(courseService.create(newCourseRequest)).build());
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'TEACHER')")
  public ResponseEntity<CoursesPagedResources> getAllCourses(
      final Pageable pageable,
      final String name,
      final CourseLevelType level,
      final CourseCategoryType category,
      final String instructorId) {
    final Page<CourseResource> page =
        courseService.getAll(name, level, category, instructorId, pageable);

    return ResponseEntity.ok(
        CoursesPagedResources.builder()
            .data(
                CoursesData.builder()
                    .content(page.getContent())
                    .size(page.getNumberOfElements())
                    .build())
            .page(PageResource.builder().number(page.getNumber()).size(page.getSize()).build())
            .totalElements(page.getTotalElements())
            .build());
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR') or @courseSecurity.isOwner(#courseId, authentication)")
  public ResponseEntity<CourseResponse> updateCourse(
      final String courseId, final UpdateCourseRequest updateCourseRequest) {
    return ResponseEntity.ok(
            CourseResponse.builder()
                    .course(courseService.update(courseId, updateCourseRequest))
                    .build());
  }

  @Override
  @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'TEACHER')")
  public ResponseEntity<CourseResponse> getCourse(final String courseId) {
    return ResponseEntity.ok(
            CourseResponse.builder()
                    .course(courseService.getById(courseId))
                    .build());
  }

  @Override
  @PreAuthorize("hasRole('ADMINISTRATOR')")
  public ResponseEntity<Void> deleteCourse(final String courseId) {
    courseService.delete(courseId);
    return ResponseEntity.noContent().build();
  }
}