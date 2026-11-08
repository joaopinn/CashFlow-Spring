package com.cashflow.demo.dto.category;

import com.cashflow.demo.dto.transactiontype.TransactionTypeResponse;

public record CategoryResponse(
        Long id,
        String title,
        TransactionTypeResponse type
) {}
