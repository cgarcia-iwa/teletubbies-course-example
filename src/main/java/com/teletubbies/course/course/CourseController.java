package com.teletubbies.course.course;

import com.teletubbies.course.CoursesApi;
import com.teletubbies.course.model.CourseResource;
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
  public ResponseEntity<CourseResource> createCourse(final NewCourseRequest newCourseRequest) {
    return ResponseEntity.status(HttpStatus.CREATED).body(courseService.create(newCourseRequest));
  }

  @Override
  public ResponseEntity<List<CourseResource>> getAllCourses() {
    return ResponseEntity.ok(courseService.getAll());
  }

  @Override
  public ResponseEntity<CourseResource> updateCourse(
      final String courseId, final UpdateCourseRequest updateCourseRequest) {
    return ResponseEntity.ok(courseService.update(courseId, updateCourseRequest));
  }

  @Override
  public ResponseEntity<CourseResource> getCourse(final String courseId) {
    return ResponseEntity.ok(courseService.getById(courseId));
  }

  @Override
  public ResponseEntity<Void> deleteCourse(final String courseId) {
    courseService.delete(courseId);
    return ResponseEntity.noContent().build();
  }
}
