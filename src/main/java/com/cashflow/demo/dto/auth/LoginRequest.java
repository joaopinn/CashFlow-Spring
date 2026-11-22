package com.cashflow.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import com.cashflow.demo.validation.PasswordBytes;

public record LoginRequest(
        @NotBlank @Email @Size(max = 255) @Schema(example = "maria@example.com") String email,
        @NotBlank @Size(max = 72) @PasswordBytes @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String password
) {}
