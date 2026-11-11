package com.cashflow.demo.services;

import com.cashflow.demo.dto.transaction.TransactionRequest;
import com.cashflow.demo.dto.transaction.TransactionResponse;
import com.cashflow.demo.entity.TransactionModel;
import com.cashflow.demo.mapper.TransactionMapper;
import com.cashflow.demo.repository.CategoryRepository;
import com.cashflow.demo.repository.TransactionRepository;
import com.cashflow.demo.repository.TransactionTypeRepository;
import com.cashflow.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class TransactionService {
    private final TransactionRepository repository;
    private final TransactionMapper mapper;
    private final TransactionTypeRepository typeRepository;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;

    public TransactionService(TransactionRepository repository,
                              UserRepository userRepository,
                              TransactionMapper mapper,
                              TransactionTypeRepository typeRepository,
                              CategoryRepository categoryRepository){
        this.mapper = mapper;
        this.repository = repository;
        this.userRepository = userRepository;
        this.typeRepository = typeRepository;
        this.categoryRepository = categoryRepository;
    }

    // findAll

    public List<TransactionResponse> findAll(Long userId){
        requireUser(userId);
        return repository.findAllByUser_Id(userId).stream().map(mapper::response).toList();
    }

    // findByName

    public List<TransactionResponse> findByName(Long userId,
                                          String name){
        requireUser(userId);
        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome da categoria");
        }

        return repository.findByUser_IdAndTitleContainingIgnoreCase(userId, name.strip())
                .stream().map(mapper::response).toList();
    }

    // create

    @Transactional
    public TransactionResponse create(Long userId, TransactionRequest request) {
        var user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Usuário não encontrado: " + userId
                ));

        var type = typeRepository.findById(request.typeId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tipo de transação não encontrado: " + request.typeId()
                ));
        var category = categoryRepository
                .findByIdAndUser_Id(request.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        TransactionModel transactionModel =
                mapper.toEntity(request, type, category);
        transactionModel.setUser(user);

        return mapper.response(repository.save(transactionModel));
    }

    // update

    @Transactional
    public TransactionResponse update(Long userId, TransactionRequest request){
        requireUser(userId);

        var type = typeRepository.findById(request.typeId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tipo de transação não encontrado: " + request.typeId()
                ));
        var category = categoryRepository
                .findByIdAndUser_Id(request.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        TransactionModel transactionModel = mapper.toEntity(request, type, category);

        return mapper.response(repository.save(transactionModel));
    }

    // delete

    public void delete(Long userId, Long id){
        requireUser(userId);

        TransactionModel transaction = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada: " + id));

        repository.delete(transaction);
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId);
        }
    }
}
