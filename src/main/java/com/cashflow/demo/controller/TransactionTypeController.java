package com.cashflow.demo.controller;

import com.cashflow.demo.dto.transactiontype.*;
import com.cashflow.demo.services.TransactionTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.*;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.net.URI;

@RestController
@RequestMapping("/transaction-types")
@Tag(name = "Tipos de transação", description = "Catálogo global. Leitura autenticada; alterações somente por ADMIN")
public class TransactionTypeController {
    private final TransactionTypeService service;
    public TransactionTypeController(TransactionTypeService service) { this.service = service; }

    @GetMapping
    @Operation(summary = "Listar tipos com paginação")
    public ResponseEntity<Page<TransactionTypeResponse>> findAll(
            @ParameterObject @PageableDefault(size = 20, sort = "title") Pageable pageable) {
        return ResponseEntity.ok(service.findAll(pageable));
    }
    @GetMapping("/{id}")
    @Operation(summary = "Buscar tipo pelo ID")
    public ResponseEntity<TransactionTypeResponse> findById(@PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.findById(id));
    }
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar tipo (ADMIN)")
    public ResponseEntity<TransactionTypeResponse> create(@Valid @RequestBody TransactionTypeRequest request) {
        var response = service.create(request);
        return ResponseEntity.created(URI.create("/transaction-types/" + response.id())).body(response);
    }
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Atualizar tipo pelo ID (ADMIN)")
    public ResponseEntity<TransactionTypeResponse> update(@PathVariable @Positive Long id,
                                                         @Valid @RequestBody TransactionTypeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Excluir tipo (ADMIN)")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
