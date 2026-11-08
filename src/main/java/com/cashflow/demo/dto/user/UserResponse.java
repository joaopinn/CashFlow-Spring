package com.cashflow.demo.dto.user;

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
