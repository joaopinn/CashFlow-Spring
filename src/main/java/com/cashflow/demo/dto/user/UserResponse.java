package com.cashflow.demo.dto.user;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Dados públicos de perfil. Senhas e hashes nunca são retornados.")
public record UserResponse(
        Long id,
        String name,
        String cnpj,
        String segment,
        String email,
        String phone,
        String role
) {

}
