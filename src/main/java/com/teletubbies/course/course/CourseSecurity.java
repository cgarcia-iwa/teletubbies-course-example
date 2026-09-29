package com.teletubbies.course.course;

import com.teletubbies.course.instructor.InstructorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Component("courseSecurity")
@RequiredArgsConstructor
public class CourseSecurity {

    private final CourseRepository courseRepository;
    private final InstructorRepository instructorRepository;

    @Transactional(readOnly = true)
    public boolean isOwner(final String courseId, final Authentication authentication) {
        return instructorRepository
                .findByEmail(authentication.getName())
                .flatMap(instructor -> courseRepository.findById(UUID.fromString(courseId))
                        .map(course -> course.getInstructorId().equals(instructor.getId())))
                .orElse(false);
    }
}