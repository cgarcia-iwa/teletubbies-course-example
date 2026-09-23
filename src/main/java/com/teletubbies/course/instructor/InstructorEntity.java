package com.teletubbies.course.instructor;

import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Entity
@Table(
    name = "instructor",
    uniqueConstraints = {
      @UniqueConstraint(name = "instructor_full_name_uk", columnNames = "full_name"),
      @UniqueConstraint(name = "instructor_email_uk", columnNames = "email")
    })
@EqualsAndHashCode(of = "id")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@ToString
public class InstructorEntity implements Serializable {
  @Serial private static final long serialVersionUID = 1L;

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  @Column(nullable = false, unique = true)
  private UUID id;

  @Column(name = "full_name", length = 150, nullable = false)
  private String fullName;

  @Column(name = "email", length = 150, nullable = false)
  private String email;

  public InstructorEntity(final NewInstructorRequest request) {
    this.fullName = request.getFullName();
    this.email = request.getEmail();
  }

  public void update(final UpdateInstructorRequest request) {
    this.fullName = request.getFullName();
    this.email = request.getEmail();
  }
}
