package com.teletubbies.course.instructor;

import com.teletubbies.course.model.NewInstructorRequest;
import com.teletubbies.course.model.UpdateInstructorRequest;
import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Converter;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.io.Serial;
import java.io.Serializable;
import java.util.Optional;
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
@ToString(exclude = "password")
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

  // Hash BCrypt, never the plain-text password. Null = the instructor cannot log in.
  @Column(name = "password", length = 100)
  private String password;

  @Column(name = "role", nullable = false, columnDefinition = "smallint")
  private InstructorRoleType role;

  public InstructorEntity(final NewInstructorRequest request, final String passwordHash) {
    this.fullName = request.getFullName();
    this.email = request.getEmail();
    this.password = passwordHash;
    this.role = Optional.ofNullable(request.getRole()).orElse(InstructorRoleType.TEACHER);
  }

  public InstructorEntity(
      final String fullName,
      final String email,
      final String passwordHash,
      final InstructorRoleType role) {
    this.fullName = fullName;
    this.email = email;
    this.password = passwordHash;
    this.role = role;
  }

  public void update(final UpdateInstructorRequest request) {
    this.fullName = request.getFullName();
    this.email = request.getEmail();
  }

  public void changePassword(final String passwordHash) {
    this.password = passwordHash;
  }

  public void changeRole(final InstructorRoleType role) {
    this.role = role;
  }
}