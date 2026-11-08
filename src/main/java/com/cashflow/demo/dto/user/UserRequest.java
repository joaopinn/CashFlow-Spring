package com.cashflow.demo.dto.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record UserRequest(
        @NotBlank String name,
        @NotBlank String cnpj,
        @NotBlank String segment,
        @NotBlank @Email String email,
        @NotBlank String password,
        @NotBlank String phone,
        @NotBlank String role
) {}
