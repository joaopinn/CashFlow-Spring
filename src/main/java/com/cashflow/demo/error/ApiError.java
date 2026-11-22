package com.cashflow.demo.error;

import io.swagger.v3.oas.annotations.media.Schema;
import java.time.Instant;
import java.util.List;

@Schema(description = "Resposta única de erro; nunca inclui senha, token, SQL ou stack trace")
public record ApiError(Instant timestamp, int status, String error, String message,
                       String path, List<FieldError> fieldErrors) {
    public record FieldError(String field, String message) {}
}
