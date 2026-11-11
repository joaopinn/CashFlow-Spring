package com.cashflow.demo.controller;


import com.cashflow.demo.dto.transaction.TransactionRequest;
import com.cashflow.demo.dto.transaction.TransactionResponse;
import com.cashflow.demo.services.TransactionService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/transactions")
public class TransactionController {

    private final TransactionService service;

    public TransactionController(TransactionService service){
        this.service = service;
    };

    // GET findAll

    @GetMapping
    public List<TransactionResponse> findAll(@PathVariable Long userId){
        return service.findAll(userId);
    }

    // GET findByName

    @GetMapping(params = "name")
    public List<TransactionResponse> findByName(@PathVariable Long userId, String name){
        return service.findByName(userId, name);
    }

    // POST create

    @PostMapping
    public TransactionResponse create(@PathVariable Long userId, TransactionRequest request){
        return service.create(userId, request);
    }

    // PUT update

    @PutMapping
    public TransactionResponse update(@PathVariable Long userId, TransactionRequest request){
        return service.update(userId, request);
    }

    // DELETE delete

    @DeleteMapping
    public void delete(@PathVariable Long userId, Long id){
        service.delete(userId, id);
    }
}
