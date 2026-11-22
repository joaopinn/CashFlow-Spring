package com.cashflow.demo.controller;

import com.cashflow.demo.dto.user.UserRequest;
import com.cashflow.demo.dto.user.UserResponse;
import com.cashflow.demo.services.UserService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;

@Tag(name = "Usuários")
@RestController
@RequestMapping("/users")
public class UserController {
    private final UserService service;

    public UserController(UserService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Listar com paginação", description = "Listagem, busca e criação exigem ADMIN. Demais operações: próprio usuário ou ADMIN.")
    public ResponseEntity<Page<UserResponse>> findAll(@ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }

    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Pesquisar com filtros combináveis", description = "ADMIN. Filtros opcionais: name, email, segment (parciais, ignoram caixa) e role (exato, ignora caixa). Combinados com AND.")
    public ResponseEntity<Page<UserResponse>> search(
            @RequestParam(required = false) @Size(max = 255) String name,
            @RequestParam(required = false) String email,
            @RequestParam(required = false) String segment,
            @RequestParam(required = false) String role,
            @ParameterObject @PageableDefault(size = 20, sort = "name") Pageable pageable) {
        return ResponseEntity.ok(service.search(name, email, segment, role, pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("@accessPolicy.canAccess(#id, authentication)")
    @Operation(summary = "Buscar pelo ID", description = "Listagem, busca e criação exigem ADMIN. Demais operações: próprio usuário ou ADMIN.")
    public ResponseEntity<UserResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar registro", description = "Listagem, busca e criação exigem ADMIN. Demais operações: próprio usuário ou ADMIN.")
    public ResponseEntity<UserResponse> create(@Valid @RequestBody UserRequest request) {
        UserResponse response = service.create(request);
        return ResponseEntity.created(URI.create("/users/" + response.id())).body(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@accessPolicy.canAccess(#id, authentication)")
    @Operation(summary = "Atualizar registro existente pelo ID", description = "Listagem, busca e criação exigem ADMIN. Demais operações: próprio usuário ou ADMIN.")
    public ResponseEntity<UserResponse> update(@PathVariable @Positive Long id, @Valid @RequestBody UserRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@accessPolicy.canAccess(#id, authentication)")
    @Operation(summary = "Excluir pelo ID", description = "Listagem, busca e criação exigem ADMIN. Demais operações: próprio usuário ou ADMIN.")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
