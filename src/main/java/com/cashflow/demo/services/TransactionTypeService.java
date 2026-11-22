package com.cashflow.demo.services;

import com.cashflow.demo.dto.transactiontype.*;
import com.cashflow.demo.entity.TransactionTypeModel;
import com.cashflow.demo.mapper.TransactionTypeMapper;
import com.cashflow.demo.repository.TransactionTypeRepository;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class TransactionTypeService {
    private final TransactionTypeRepository repository;
    private final TransactionTypeMapper mapper;
    public TransactionTypeService(TransactionTypeRepository repository, TransactionTypeMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }
    public Page<TransactionTypeResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::response);
    }
    public TransactionTypeResponse findById(Long id) { return mapper.response(find(id)); }
    @Transactional
    public TransactionTypeResponse create(TransactionTypeRequest request) {
        var type = new TransactionTypeModel();
        type.setTitle(request.title());
        return mapper.response(repository.save(type));
    }
    @Transactional
    public TransactionTypeResponse update(Long id, TransactionTypeRequest request) {
        var type = find(id);
        type.setTitle(request.title());
        return mapper.response(repository.save(type));
    }
    @Transactional
    public void delete(Long id) { repository.delete(find(id)); }
    private TransactionTypeModel find(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Tipo de transação não encontrado: " + id));
    }
}
