package com.cashflow.demo.dto.auth;

import io.swagger.v3.oas.annotations.media.Schema;

public record TokenResponse(@Schema(description = "JWT assinado; usar no cabeçalho Authorization") String accessToken,
                            @Schema(example = "Bearer") String tokenType,
                            @Schema(description = "Validade em segundos", example = "900") long expiresIn,
                            Long userId) {}
