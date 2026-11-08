package com.cashflow.demo.mapper;

import com.cashflow.demo.dto.user.UserRequest;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;

class UserMapperTest {
    @Test
    void storesPasswordAsVerifiableHash() {
        var encoder = new BCryptPasswordEncoder();
        var mapper = new UserMapper(encoder);
        var request = new UserRequest("Maria", "12345678000190", "Serviços",
                "maria@example.com", "senha-de-teste", "71999999999", "Gestora");

        var user = mapper.toEntity(request);

        assertNotEquals(request.password(), user.getPassword());
        assertTrue(encoder.matches(request.password(), user.getPassword()));
        assertFalse(encoder.matches("senha-incorreta", user.getPassword()));
    }
}
