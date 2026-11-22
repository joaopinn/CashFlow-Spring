package com.cashflow.demo.controller;


import com.cashflow.demo.dto.transaction.TransactionRequest;
import com.cashflow.demo.dto.transaction.TransactionResponse;
import com.cashflow.demo.services.TransactionService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.security.access.prepost.PreAuthorize;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

import java.time.LocalDate;

@Tag(name = "Transações")
@RestController
@RequestMapping("/users/{userId}/transactions")
@PreAuthorize("@accessPolicy.canAccess(#userId, authentication)")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service){
        this.service = service;
    }

    // GET findAll

    @GetMapping({"", "/getAllTransaction"})
    @Operation(summary = "Listar com paginação", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<Page<TransactionResponse>> findAll(
            @PathVariable @Positive Long userId,
            @ParameterObject @PageableDefault(size = 20, sort = "date")
            Pageable pageable){
        return ResponseEntity.ok(
                service.findAll(userId, pageable)
        );
    }

    @GetMapping("/{id}")
    @Operation(summary = "Buscar transação pelo ID", description = "Somente o proprietário ou ADMIN")
    public ResponseEntity<TransactionResponse> findById(@PathVariable @Positive Long userId,
                                                       @PathVariable @Positive Long id) {
        return ResponseEntity.ok(service.findById(userId, id));
    }

    // GET search
    @GetMapping("/search")
    @Operation(summary = "Pesquisar com filtros combináveis", description = "Filtros opcionais: name (descrição parcial), typeId, startDate e endDate (yyyy-MM-dd, limites inclusivos). Todos combinados com AND. Sem filtros lista todos do usuário.")
    public ResponseEntity<Page<TransactionResponse>> search(
            @PathVariable @Positive Long userId,

            @RequestParam(required = false)
            @Size(max = 255) String name,

            @RequestParam(required = false)
            @Positive Long typeId,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate startDate,

            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate endDate,

            @ParameterObject @PageableDefault(size = 20, sort = "date") Pageable pageable){
        return ResponseEntity.ok(service.search(userId, name, typeId, startDate, endDate, pageable));
    }

    // POST create
    @PostMapping({"", "/create"})
    @Operation(summary = "Criar registro", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<TransactionResponse> create(@PathVariable @Positive Long userId,
                                                     @Valid @RequestBody TransactionRequest request){
        TransactionResponse response = service.create(userId, request);
        return ResponseEntity.created(URI.create("/users/" + userId + "/transactions/" + response.id()))
                .body(response);
    }

    // PUT update

    @PutMapping("/{id}")
    @Operation(summary = "Atualizar registro existente pelo ID", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<TransactionResponse> update(@PathVariable @Positive Long userId,
                                                     @PathVariable @Positive Long id,
                                                     @Valid @RequestBody TransactionRequest request){
        return ResponseEntity.ok(service.update(userId, id, request));
    }

    // DELETE delete

    @DeleteMapping({"/{id}", "/delete/{id}"})
    @Operation(summary = "Excluir pelo ID", description = "Somente o proprietário da rota ou ADMIN. Dados de outros usuários não são retornados.")
    public ResponseEntity<Void> delete(@PathVariable @Positive Long userId, @PathVariable @Positive Long id){
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
