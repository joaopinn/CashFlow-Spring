package com.cashflow.demo.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

import com.cashflow.demo.dto.transactiontype.TransactionTypeResponse;

@Schema(description = "Categoria com o tipo de transação correspondente.")
public record CategoryResponse(
        Long id,
        String title,
        TransactionTypeResponse type
) {}
