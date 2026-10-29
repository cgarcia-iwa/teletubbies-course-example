package com.teletubbies.course.course;

import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component("courseSecurity")
@RequiredArgsConstructor
public class CourseSecurity {

    private final CourseRepository courseRepository;

    /**
     * Whether the authenticated instructor owns the course. An id that is not a UUID cannot belong
     * to anyone, so it answers {@code false} (403) instead of letting the parse error escape the
     * {@code @PreAuthorize} evaluation as a 500.
     */
    @Transactional(readOnly = true)
    public boolean isOwner(final String courseId, final Authentication authentication) {
        return CourseService.tryParseUuid(courseId)
                .map(id -> courseRepository.existsByIdAndInstructor_Email(id, authentication.getName()))
                .orElse(false);
    }
}
