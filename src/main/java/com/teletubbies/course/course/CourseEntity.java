package com.teletubbies.course.course;

import com.teletubbies.course.instructor.InstructorEntity;
import com.teletubbies.course.model.NewCourseRequest;
import com.teletubbies.course.model.UpdateCourseRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.CascadeType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import lombok.*;

@Entity
@Table(
    name = "course",
    uniqueConstraints = @UniqueConstraint(name = "course_name_uk", columnNames = "name"))
@EqualsAndHashCode(of = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString(exclude = "instructor")
public class CourseEntity implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, unique = true)
  private UUID id;

  @Column(name = "name", length = 100, nullable = false)
  private String name;

  @Column(name = "description", length = 200)
  private String description;

  @Column(name = "duration", nullable = false)
  private Short duration;

  @Column(name = "level", nullable = false, columnDefinition = "smallint")
  private CourseLevelType level;

  @Column(name = "category", nullable = false, columnDefinition = "smallint")
  private CourseCategoryType category;


  @Column(name = "instructor_id", nullable = false)
  private UUID instructorId;

  @Setter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "instructor_id", nullable = false, insertable = false, updatable = false)
  private InstructorEntity instructor;

  public CourseEntity(final NewCourseRequest request) {
    this.name = request.getName();
    this.description = request.getDescription();
    this.duration = request.getDuration().shortValue();
    this.level = request.getLevel();
    this.category = request.getCategory();
    this.instructorId = UUID.fromString(request.getInstructorId());
  }

  public void update(final UpdateCourseRequest request) {
    this.name = request.getName();
    this.description = request.getDescription();
    this.duration = request.getDuration().shortValue();
    this.level = request.getLevel();
    this.category = request.getCategory();
    this.instructorId = UUID.fromString(request.getInstructorId());
  }
}
