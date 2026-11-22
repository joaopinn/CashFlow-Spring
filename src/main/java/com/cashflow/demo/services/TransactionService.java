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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Locale;

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

    public Page<TransactionResponse> findAll(Long userId, Pageable pageable){
        requireUser(userId);
        return repository.findAllByUser_Id(userId, pageable).map(mapper::response);
    }

    public TransactionResponse findById(Long userId, Long id) {
        requireUser(userId);
        return mapper.response(repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada: " + id)));
    }

    public Page<TransactionResponse> search(Long userId, String name, Long typeId,
                                            LocalDate startDate, LocalDate endDate,
                                            Pageable pageable) {
        requireUser(userId);
        if (typeId != null && typeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID do tipo deve ser positivo");
        }
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "A data inicial deve ser anterior ou igual à data final");
        }

        return repository.findAll((root, query, cb) -> {
            var filters = new ArrayList<Predicate>();
            filters.add(cb.equal(root.get("user").get("id"), userId));
            if (name != null && !name.isBlank()) {
                String term = name.strip().toLowerCase(Locale.ROOT)
                        .replace("!", "!!").replace("%", "!%").replace("_", "!_");
                filters.add(cb.like(cb.lower(root.get("description")), "%" + term + "%", '!'));
            }
            if (typeId != null) {
                filters.add(cb.equal(root.get("type").get("id"), typeId));
            }
            if (startDate != null) {
                filters.add(cb.greaterThanOrEqualTo(root.<LocalDate>get("date"), startDate));
            }
            if (endDate != null) {
                filters.add(cb.lessThanOrEqualTo(root.<LocalDate>get("date"), endDate));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        }, pageable).map(mapper::response);
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

        requireMatchingType(category.getType().getId(), request.typeId());
        TransactionModel transactionModel =
                mapper.toEntity(request, type, category);
        transactionModel.setUser(user);

        return mapper.response(repository.save(transactionModel));
    }

    // update

    @Transactional
    public TransactionResponse update(Long userId, Long id, TransactionRequest request){
        requireUser(userId);

        TransactionModel transactionModel = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada: " + id));

        var type = typeRepository.findById(request.typeId()).orElseThrow(() ->
                new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "Tipo de transação não encontrado: " + request.typeId()
                ));
        var category = categoryRepository
                .findByIdAndUser_Id(request.categoryId(), userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "Categoria não encontrada"));

        requireMatchingType(category.getType().getId(), request.typeId());
        mapper.updateEntity(request, transactionModel, type, category);

        return mapper.response(repository.save(transactionModel));
    }

    // delete

    @Transactional
    public void delete(Long userId, Long id){
        requireUser(userId);

        TransactionModel transaction = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Transação não encontrada: " + id));

        repository.delete(transaction);
    }

    private void requireMatchingType(Long categoryTypeId, Long transactionTypeId) {
        if (!java.util.Objects.equals(categoryTypeId, transactionTypeId)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "O tipo da transação deve ser igual ao tipo da categoria");
        }
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId);
        }
    }
}
