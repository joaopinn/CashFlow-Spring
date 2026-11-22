package com.cashflow.demo.controller;

import com.cashflow.demo.dto.auth.*;
import com.cashflow.demo.dto.user.UserResponse;
import com.cashflow.demo.services.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/auth")
@Tag(name = "Autenticação", description = "Cadastro público e emissão de JWT")
@SecurityRequirements
public class AuthController {
    private final AuthService auth;
    private final UserService users;
    public AuthController(AuthService auth, UserService users) { this.auth = auth; this.users = users; }

    @PostMapping("/register")
    @Operation(summary = "Cadastrar usuário", description = "Público. Cria somente USER; e-mail deve ser único. Não retorna senha")
    public ResponseEntity<UserResponse> register(@Valid @RequestBody RegisterRequest request) {
        var response = users.create(request.toUserRequest());
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(response);
    }

    @PostMapping("/login")
    @Operation(summary = "Autenticar por e-mail e senha", description = "Retorna JWT Bearer com expiração. Credenciais incorretas retornam 401")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok().cacheControl(CacheControl.noStore()).header("Pragma", "no-cache")
                .body(auth.login(request));
    }
}
