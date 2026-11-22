package com.cashflow.demo.dto.transactiontype;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Tipo de transação do catálogo global.")
public record TransactionTypeResponse(
        Long id,
        String title
) {
}
