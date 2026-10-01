package com.teletubbies.course.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import java.net.URI;
import java.util.List;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * Turns every exception handled by the API into an RFC 9457 {@link ProblemDetail}.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String TYPE_BASE_URL = "https://api.teletubbies.dev/problems/";
    public static final String PROP_TRACE_ID = "traceId";
    public static final String PROP_ERRORS = "errors";

    public static final String TYPE_UNAUTHORIZED = TYPE_BASE_URL + "unauthorized";
    public static final String DETAIL_UNAUTHORIZED =
            "Full authentication is required to access this resource";



    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail handleResponseStatusException(ResponseStatusException e, HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(e.getStatusCode().value());
        String detail = e.getReason() != null ? e.getReason() : status.getReasonPhrase();
        return createProblem(status, detail, request, e);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleMethodArgumentNotValid(MethodArgumentNotValidException e, HttpServletRequest request) {
        var errors = e.getBindingResult().getFieldErrors().stream()
                .map(err -> new FieldViolation(err.getField(), err.getDefaultMessage()))
                .toList();
        return createProblem(HttpStatus.BAD_REQUEST, "The request contains invalid or missing fields", request, e, errors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ProblemDetail handleConstraintViolation(ConstraintViolationException e, HttpServletRequest request) {
        var errors = e.getConstraintViolations().stream()
                .map(v -> new FieldViolation(String.valueOf(v.getPropertyPath()), v.getMessage()))
                .toList();
        return createProblem(HttpStatus.BAD_REQUEST, "The request contains invalid or missing parameters", request, e, errors);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleHttpMessageNotReadable(HttpMessageNotReadableException e, HttpServletRequest request) {
        return createProblem(HttpStatus.BAD_REQUEST, "The request body is missing or is not valid JSON", request, e);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        var errors = List.of(new FieldViolation(e.getName(), "The value is not of the expected type"));
        return createProblem(HttpStatus.BAD_REQUEST, "The request contains a parameter with an invalid value", request, e, errors);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ProblemDetail handleMissingServletRequestParameter(MissingServletRequestParameterException e, HttpServletRequest request) {
        var errors = List.of(new FieldViolation(e.getParameterName(), "The parameter is required"));
        return createProblem(HttpStatus.BAD_REQUEST, "The request is missing a required parameter", request, e, errors);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException e, HttpServletRequest request) {
        return createProblem(HttpStatus.FORBIDDEN, "You do not have permission to perform this operation", request, e);
    }

    @ExceptionHandler({NoResourceFoundException.class, NoHandlerFoundException.class})
    public ProblemDetail handleNotFound(Exception e, HttpServletRequest request) {
        return createProblem(HttpStatus.NOT_FOUND, "The requested endpoint does not exist", request, e);
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ProblemDetail handleMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        return createProblem(HttpStatus.METHOD_NOT_ALLOWED, "The HTTP method is not supported by this endpoint", request, e);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrityViolation(DataIntegrityViolationException e, HttpServletRequest request) {
        return createProblem(HttpStatus.CONFLICT, "The request conflicts with the current state of the resource", request, e);
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception e, HttpServletRequest request) {
        return createProblem(HttpStatus.INTERNAL_SERVER_ERROR, "The request could not be processed due to an unexpected error", request, e);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String detail,
            HttpServletRequest request,
            Exception e
    ){
        return createProblem(status, detail, request, e, null);
    }

    private ProblemDetail createProblem(
            HttpStatus status,
            String detail,
            HttpServletRequest request,
            Exception e,
            List<FieldViolation> errors) {

        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);

        String typeSuffix = status.name().toLowerCase().replace('_', '-');
        String traceId = UUID.randomUUID().toString();

        problem.setType(URI.create(TYPE_BASE_URL + typeSuffix));
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty(PROP_TRACE_ID, traceId);

        if (errors != null && !errors.isEmpty()) {
            problem.setProperty(PROP_ERRORS, errors);
        }

        log.warn("API Error [{}]: status={} path={} exception={} detail={}",
                traceId, status.value(), request.getRequestURI(), e.getClass().getSimpleName(), detail);

        return problem;
    }
}