package com.cashflow.demo.controller;

import com.cashflow.demo.dto.category.CategoryRequest;
import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.services.CategoryService;
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

@Tag(name = "Categorias")
@RestController
@RequestMapping("/users/{userId}/categories")
@PreAuthorize("@accessPolicy.canAccess(#userId, authentication)")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Listar com paginação", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<Page<CategoryResponse>> findAll(@PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(userId, pageable));
    }

    @GetMapping("/search")
    @Operation(summary = "Pesquisar com filtros combináveis", description = "Filtros opcionais: name (título parcial) e typeId, combinados com AND. Restringe sempre ao usuário da rota.")
    public ResponseEntity<Page<CategoryResponse>> search(
            @PathVariable @Positive Long userId,
            @RequestParam(required = false) @Size(max = 255) String name,
            @RequestParam(required = false) @Positive Long typeId,
            @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(service.search(userId, name, typeId, pageable));
    }

    @GetMapping(params = "name")
    @Operation(summary = "Buscar por nome (alias da busca)", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<Page<CategoryResponse>> findByName(
            @PathVariable @Positive Long userId, @RequestParam String name,
            @RequestParam(required = false) @Positive Long typeId,
            @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(service.search(userId, name, typeId, pageable));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar pelo ID", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<CategoryResponse> findById(@PathVariable @Positive Long userId, @PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.findById(userId, id));
    }

    @PostMapping
    @Operation(summary = "Criar registro", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<CategoryResponse> create(@PathVariable @Positive Long userId,
                                                    @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = service.create(userId, request);
        return ResponseEntity.created(URI.create("/users/" + userId + "/categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro existente pelo ID", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<CategoryResponse> update(@PathVariable @Positive Long userId, @PathVariable @Positive Long id,
                                   @Valid @RequestBody CategoryRequest request) {
        return ResponseEntity.ok(service.update(userId, id, request));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Excluir pelo ID", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long userId, @PathVariable @Positive Long id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
