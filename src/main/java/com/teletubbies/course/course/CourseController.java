package com.teletubbies.course.course;

import com.teletubbies.course.CoursesApi;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.CourseResponse;
import com.teletubbies.course.model.CoursesResponse;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CourseController implements CoursesApi {

  private final CourseService courseService;

  public CourseController(final CourseService courseService) {
    this.courseService = courseService;
  }

  @Override
  public ResponseEntity<CourseResponse> createCourse(final NewCourseRequest newCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(
            CourseResponse.builder()
                    .course(courseService.create(newCourseRequest)).build());
  }

  @Override
  public ResponseEntity<CoursesResponse> getAllCourses() {
    return ResponseEntity.ok(CoursesResponse.builder().courses(courseService.getAll()).build());
  }

  @Override
  public ResponseEntity<CourseResponse> updateCourse(
      final String courseId, final UpdateCourseRequest updateCourseRequest) {
    return ResponseEntity.ok(
            CourseResponse.builder()
                    .course(courseService.update(courseId, updateCourseRequest))
                    .build());
  }

  @Override
  public ResponseEntity<CourseResponse> getCourse(final String courseId) {
    return ResponseEntity.ok(
            CourseResponse.builder()
                    .course(courseService.getById(courseId))
                    .build());
  }

  @Override
  public ResponseEntity<Void> deleteCourse(final String courseId) {
    courseService.delete(courseId);
    return ResponseEntity.noContent().build();
  }
}
