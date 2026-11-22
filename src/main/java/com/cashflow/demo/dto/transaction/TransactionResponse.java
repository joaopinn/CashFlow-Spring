package com.cashflow.demo.dto.transaction;

import io.swagger.v3.oas.annotations.media.Schema;

import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.dto.transactiontype.TransactionTypeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "Transação com tipo e categoria. Nunca inclui dados de autenticação do usuário.")
public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionTypeResponse type,
        CategoryResponse category,
        LocalDate date,
        String status
) {}
