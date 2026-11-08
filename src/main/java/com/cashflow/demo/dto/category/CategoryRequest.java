package com.cashflow.demo.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CategoryRequest(
        @NotBlank String title,
        @NotNull @Positive Long typeId
) {}
