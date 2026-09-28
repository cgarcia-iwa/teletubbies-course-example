package com.teletubbies.course.instructor;

import java.util.Optional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AdminInstructorSeeder implements CommandLineRunner {

  private final InstructorRepository instructorRepository;
  private final PasswordEncoder passwordEncoder;

  @Value("${app.admin.full-name}")
  private final String fullName;

  @Value("${app.admin.email}")
  private final String email;

  @Value("${app.admin.password}")
  private final String password;

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
          new InstructorEntity(fullName, email, passwordEncoder.encode(password)));
      log.info("Created admin instructor '{}'", email);
      return;
    }

    final InstructorEntity instructor = existing.get();
    if (instructor.getPassword() == null) {
      instructor.changePassword(passwordEncoder.encode(password));
      instructorRepository.save(instructor);
      log.info("Assigned password to admin instructor '{}'", email);
    }
  }
}
