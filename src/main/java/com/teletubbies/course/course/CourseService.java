package com.teletubbies.course.course;

import com.teletubbies.course.model.CourseCategory;
import com.teletubbies.course.model.CourseLevel;
import com.teletubbies.course.model.CourseResource;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class CourseService {

  private final Map<String, CourseResource> courses = new ConcurrentHashMap<>();

  public CourseService() {
    seedCourses();
  }

  public List<CourseResource> getAll() {
    return List.copyOf(courses.values());
  }

  public CourseResource create(final NewCourseRequest request) {
    final CourseResource course =
        CourseResource.builder()
            .id(UUID.randomUUID().toString())
            .name(request.getName())
            .description(request.getDescription())
            .duration(request.getDuration())
            .level(request.getLevel())
            .category(request.getCategory())
            .build();
    courses.put(course.getId(), course);
    return course;
  }

  public CourseResource update(final String courseId, final UpdateCourseRequest request) {
    findByIdOrThrow(courseId);
    final CourseResource updated =
        CourseResource.builder()
            .id(courseId)
            .name(request.getName())
            .description(request.getDescription())
            .duration(request.getDuration())
            .level(request.getLevel())
            .category(request.getCategory())
            .build();
    courses.put(courseId, updated);
    return updated;
  }

  public void delete(final String courseId) {
    findByIdOrThrow(courseId);
    courses.remove(courseId);
  }

  private CourseResource findByIdOrThrow(final String courseId) {
    final CourseResource course = courses.get(courseId);
    if (course == null) {
      throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Course not found: " + courseId);
    }
    return course;
  }

  private void seedCourses() {
    List.of(
            CourseResource.builder()
                .id("uno")
                .name("Introduction to Spring Boot")
                .description("Learn the fundamentals of building REST APIs with Spring Boot.")
                .duration(12)
                .level(CourseLevel.BEGINNER)
                .category(CourseCategory.PROGRAMMING)
                .build(),
            CourseResource.builder()
                .id("dos")
                .name("UI Design with Figma")
                .description("UI/UX design principles applied with Figma.")
                .duration(8)
                .level(CourseLevel.INTERMEDIATE)
                .category(CourseCategory.DESIGN)
                .build(),
            CourseResource.builder()
                .id("tres")
                .name("Business English")
                .description("Vocabulary and conversation focused on business environments.")
                .duration(20)
                .level(CourseLevel.ADVANCED)
                .category(CourseCategory.LANGUAGES)
                .build())
        .forEach(course -> courses.put(course.getId(), course));
  }
}
