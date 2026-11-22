package com.cashflow.demo.services;

import com.cashflow.demo.dto.auth.*;
import com.cashflow.demo.repository.UserRepository;
import com.cashflow.demo.security.JwtProperties;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class AuthService {
    private final UserRepository users;
    private final PasswordEncoder passwords;
    private final JwtEncoder encoder;
    private final JwtProperties properties;
    private final String dummyHash;

    public AuthService(UserRepository users, PasswordEncoder passwords, JwtEncoder encoder, JwtProperties properties) {
        this.users = users;
        this.passwords = passwords;
        this.encoder = encoder;
        this.properties = properties;
        this.dummyHash = passwords.encode(java.util.UUID.randomUUID().toString());
    }

    @Transactional(readOnly = true)
    public TokenResponse login(LoginRequest request) {
        var user = users.findByEmailIgnoreCase(request.email().strip().toLowerCase(Locale.ROOT));
        boolean matches = passwords.matches(request.password(), user.map(u -> u.getPassword()).orElse(dummyHash));
        if (user.isEmpty() || !matches) throw new BadCredentialsException("Credenciais inválidas");
        var now = Instant.now();
        var claims = JwtClaimsSet.builder().issuer(properties.issuer()).audience(List.of(properties.audience()))
                .subject(user.get().getId().toString()).issuedAt(now).notBefore(now)
                .expiresAt(now.plusSeconds(properties.ttlSeconds())).id(java.util.UUID.randomUUID().toString()).build();
        var header = JwsHeader.with(MacAlgorithm.HS256).build();
        String token = encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenResponse(token, "Bearer", properties.ttlSeconds(), user.get().getId());
    }
}
