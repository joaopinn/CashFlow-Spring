package com.cashflow.demo.dto.transactiontype;

import jakarta.validation.constraints.NotBlank;

public record TransactionTypeRequest(
        @NotBlank String title
) {}
