package com.cashflow.demo.controller;

import com.cashflow.demo.dto.category.CategoryRequest;
import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.services.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/users/{userId}/categories")
public class CategoryController {
    private final CategoryService service;

    public CategoryController(CategoryService service) {
        this.service = service;
    }

    @GetMapping
    public List<CategoryResponse> findAll(@PathVariable Long userId) {
        return service.findAll(userId);
    }

    @GetMapping(params = "name")
    public List<CategoryResponse> findByName(@PathVariable Long userId, @RequestParam String name) {
        return service.findByName(userId, name);
    }

    @GetMapping("/{id}")
    public CategoryResponse findById(@PathVariable Long userId, @PathVariable Long id) {
        return service.findById(userId, id);
    }

    @PostMapping
    public ResponseEntity<CategoryResponse> create(@PathVariable Long userId,
                                                    @Valid @RequestBody CategoryRequest request) {
        CategoryResponse response = service.create(userId, request);
        return ResponseEntity.created(URI.create("/users/" + userId + "/categories/" + response.id()))
                .body(response);
    }

    @PutMapping("/{id}")
    public CategoryResponse update(@PathVariable Long userId, @PathVariable Long id,
                                   @Valid @RequestBody CategoryRequest request) {
        return service.update(userId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long userId, @PathVariable Long id) {
        service.delete(userId, id);
        return ResponseEntity.noContent().build();
    }
}
