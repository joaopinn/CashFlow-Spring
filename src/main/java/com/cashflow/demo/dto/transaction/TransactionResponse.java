package com.cashflow.demo.dto.transaction;

import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.dto.transactiontype.TransactionTypeResponse;
import java.math.BigDecimal;
import java.time.LocalDate;

public record TransactionResponse(
        Long id,
        String description,
        BigDecimal amount,
        TransactionTypeResponse type,
        CategoryResponse category,
        LocalDate date,
        String status
) {}
