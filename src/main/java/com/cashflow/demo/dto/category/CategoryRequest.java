package com.cashflow.demo.dto.category;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Categoria vinculada a um usuário e a um tipo de transação global. Todos os campos são obrigatórios.")
public record CategoryRequest(
        @NotBlank @Size(max = 255) String title,
        @NotNull @Positive Long typeId
) {}
