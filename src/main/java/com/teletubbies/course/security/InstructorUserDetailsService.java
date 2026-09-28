package com.teletubbies.course.security;

import com.teletubbies.course.instructor.InstructorEntity;
import com.teletubbies.course.instructor.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InstructorUserDetailsService implements UserDetailsService {

  private final InstructorRepository instructorRepository;

  @Override
  @Transactional(readOnly = true)
  public UserDetails loadUserByUsername(final String email) {
    final InstructorEntity instructor =
        instructorRepository
            .findByEmail(email)
            .orElseThrow(() -> new UsernameNotFoundException("Instructor not found: " + email));

    if (instructor.getPassword() == null) {
      throw new UsernameNotFoundException("Instructor has no password: " + email);
    }

    // Class 2: add .roles(instructor.getRole().name()) here
    return User.withUsername(instructor.getEmail()).password(instructor.getPassword()).build();
  }
}
