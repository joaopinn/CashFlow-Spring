package com.cashflow.demo.error;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.core.PropertyReferenceException;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.ErrorResponse;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class ApiExceptionHandler {
    private static final Logger log = LoggerFactory.getLogger(ApiExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ApiError> validation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        var fields = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage())).toList();
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos", request, fields);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    ResponseEntity<ApiError> parameters(HandlerMethodValidationException ex, HttpServletRequest request) {
        var fields = ex.getParameterValidationResults().stream().flatMap(result -> {
            if (result instanceof org.springframework.validation.method.ParameterErrors errors) {
                return errors.getFieldErrors().stream()
                        .map(e -> new ApiError.FieldError(e.getField(), e.getDefaultMessage()));
            }
            return result.getResolvableErrors().stream().map(e -> new ApiError.FieldError(
                    result.getMethodParameter().getParameterName(), e.getDefaultMessage()));
        }).toList();
        return response(HttpStatus.BAD_REQUEST, "Parâmetros inválidos", request, fields);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ApiError> constraints(ConstraintViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Dados inválidos", request,
                ex.getConstraintViolations().stream().map(e -> new ApiError.FieldError(
                        e.getPropertyPath().toString(), e.getMessage())).toList());
    }

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ApiError> status(ResponseStatusException ex, HttpServletRequest request) {
        return response(HttpStatus.valueOf(ex.getStatusCode().value()), ex.getReason(), request, List.of());
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ApiError> authentication(AuthenticationException ex, HttpServletRequest request) {
        return response(HttpStatus.UNAUTHORIZED, "Credenciais inválidas", request, List.of());
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ApiError> denied(AccessDeniedException ex, HttpServletRequest request) {
        return response(HttpStatus.FORBIDDEN, "Acesso negado", request, List.of());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ApiError> conflict(DataIntegrityViolationException ex, HttpServletRequest request) {
        return response(HttpStatus.CONFLICT, "Operação em conflito com os dados cadastrados", request, List.of());
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, PropertyReferenceException.class, MethodArgumentTypeMismatchException.class, IllegalArgumentException.class})
    ResponseEntity<ApiError> badRequest(Exception ex, HttpServletRequest request) {
        return response(HttpStatus.BAD_REQUEST, "Corpo ou parâmetros da requisição inválidos", request, List.of());
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ApiError> unexpected(Exception ex, HttpServletRequest request) {
        if (ex instanceof ErrorResponse error) {
            var status = HttpStatus.valueOf(error.getStatusCode().value());
            var body = new ApiError(Instant.now(), status.value(), status.getReasonPhrase(),
                    status.getReasonPhrase(), request.getRequestURI(), List.of());
            return ResponseEntity.status(status).headers(error.getHeaders()).body(body);
        }
        log.error("Falha ao processar {} {}", request.getMethod(), request.getRequestURI(), ex);
        return response(HttpStatus.INTERNAL_SERVER_ERROR, "Erro interno do servidor", request, List.of());
    }

    private ResponseEntity<ApiError> response(HttpStatus status, String message,
                                              HttpServletRequest request, List<ApiError.FieldError> fields) {
        return ResponseEntity.status(status).body(new ApiError(Instant.now(), status.value(),
                status.getReasonPhrase(), message, request.getRequestURI(), fields));
    }
}
