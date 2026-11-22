package com.cashflow.demo.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Dados de uma transação. amount deve ser positivo, com até 17 dígitos inteiros e 2 decimais. O tipo deve coincidir com o da categoria pertencente ao usuário. Todos os campos são obrigatórios, inclusive no PUT.")
public record TransactionRequest(
        @NotBlank @Size(max = 255) String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Positive Long typeId,
        @NotNull @Positive Long categoryId,
        @NotNull LocalDate date,
        @NotBlank @Size(max = 50) String status
) {}
