package com.teletubbies.course.instructor;

import java.util.Optional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
public class AdminInstructorSeeder implements CommandLineRunner {

  private final InstructorRepository instructorRepository;
  private final PasswordEncoder passwordEncoder;
  private final String fullName;
  private final String email;
  private final String password;

  public AdminInstructorSeeder(
      InstructorRepository instructorRepository,
      PasswordEncoder passwordEncoder,
      @Value("${app.admin.full-name}") String fullName,
      @Value("${app.admin.email}") String email,
      @Value("${app.admin.password}") String password) {
    this.instructorRepository = instructorRepository;
    this.passwordEncoder = passwordEncoder;
    this.fullName = fullName;
    this.email = email;
    this.password = password;
  }

  @Override
  @Transactional
  public void run(final String... args) {
    if (email.isBlank() || password.isBlank()) {
      log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set, skipping admin seeding");
      return;
    }

    final Optional<InstructorEntity> existing = instructorRepository.findByEmail(email);
    if (existing.isEmpty()) {
      instructorRepository.save(
              new InstructorEntity(
                      fullName, email, passwordEncoder.encode(password), InstructorRoleType.ADMINISTRATOR));
      log.info("Created admin instructor '{}'", email);
      return;
    }

    final InstructorEntity instructor = existing.get();
    if (instructor.getPassword() == null) {
      instructor.changePassword(passwordEncoder.encode(password));
    }
    if (instructor.getRole() != InstructorRoleType.ADMINISTRATOR) {
      instructor.changeRole(InstructorRoleType.ADMINISTRATOR);
      log.info("Promoted '{}' to ADMINISTRATOR", email);
    }
    instructorRepository.save(instructor);
  }
}
