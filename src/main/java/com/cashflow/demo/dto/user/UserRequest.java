package com.cashflow.demo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import com.cashflow.demo.validation.PasswordBytes;
import org.hibernate.validator.constraints.br.CNPJ;

@Schema(description = "Dados de usuário. Apenas ADMIN pode atribuir ou alterar perfis")
public record UserRequest(
        @NotBlank @Size(max = 255) String name,
        @NotBlank @CNPJ @Size(max = 18) @Schema(example = "11222333000181") String cnpj,
        @NotBlank @Size(max = 255) String segment,
        @NotBlank @Email @Size(max = 255) @Schema(example = "maria@example.com") String email,
        @NotBlank @Size(min = 12, max = 72) @PasswordBytes @Schema(accessMode = Schema.AccessMode.WRITE_ONLY, format = "password") String password,
        @NotBlank @Pattern(regexp = "[+0-9() .-]{8,30}") String phone,
        @NotBlank @Pattern(regexp = "USER|ADMIN") @Schema(allowableValues = {"USER", "ADMIN"}) String role
) {}
