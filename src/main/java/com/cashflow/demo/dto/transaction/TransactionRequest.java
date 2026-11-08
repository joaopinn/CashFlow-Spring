package com.cashflow.demo.dto.transaction;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionRequest(
        @NotBlank String description,
        @NotNull @Positive @Digits(integer = 17, fraction = 2) BigDecimal amount,
        @NotNull @Positive Long typeId,
        @NotNull @Positive Long categoryId,
        @NotNull LocalDate date,
        @NotBlank String status
) {}
