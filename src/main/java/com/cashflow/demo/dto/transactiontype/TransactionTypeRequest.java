package com.cashflow.demo.dto.transactiontype;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Nome de um tipo do catálogo global. Escrita exclusiva de ADMIN.")
public record TransactionTypeRequest(
        @NotBlank @Size(max = 255) String title
) {}
