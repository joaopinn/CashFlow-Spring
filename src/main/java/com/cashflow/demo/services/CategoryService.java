package com.cashflow.demo.services;

import com.cashflow.demo.dto.category.CategoryRequest;
import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.entity.CategoryModel;
import com.cashflow.demo.mapper.CategoryMapper;
import com.cashflow.demo.repository.CategoryRepository;
import com.cashflow.demo.repository.TransactionTypeRepository;
import com.cashflow.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import static com.cashflow.demo.repository.specification.TextFilters.containsIgnoreCase;

@Service
@Transactional(readOnly = true)
public class CategoryService {
    private final CategoryRepository repository;
    private final CategoryMapper mapper;
    private final TransactionTypeRepository typeRepository;
    private final UserRepository userRepository;

    public CategoryService(CategoryRepository repository,
                           CategoryMapper mapper,
                           TransactionTypeRepository typeRepository,
                           UserRepository userRepository) {
        this.repository = repository;
        this.mapper = mapper;
        this.typeRepository = typeRepository;
        this.userRepository = userRepository;
    }

    public Page<CategoryResponse> findAll(Long userId, Pageable pageable) {
        requireUser(userId);
        return repository.findAllByUser_Id(userId, pageable).map(mapper::response);
    }

    public CategoryResponse findById(Long userId, Long id) {
        requireUser(userId);
        CategoryModel category = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada: " + id));
        return mapper.response(category);
    }

    public Page<CategoryResponse> search(Long userId, String name, Long typeId, Pageable pageable) {
        requireUser(userId);
        if (typeId != null && typeId <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O ID do tipo deve ser positivo");
        }
        return repository.findAll((root, query, cb) -> {
            var filters = new ArrayList<Predicate>();
            filters.add(cb.equal(root.get("user").get("id"), userId));
            if (name != null && !name.isBlank()) {
                filters.add(containsIgnoreCase(cb, root.get("title"), name));
            }
            if (typeId != null) {
                filters.add(cb.equal(root.get("type").get("id"), typeId));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        }, pageable).map(mapper::response);
    }

    @Transactional
    public CategoryResponse create(Long userId, CategoryRequest request) {
        var user = userRepository.findById(userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId));

        var type = typeRepository.findById(request.typeId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de transação não encontrado: " + request.typeId()));

        CategoryModel category = mapper.toEntity(request, type);
        category.setUser(user);

        return mapper.response(repository.save(category));
    }

    @Transactional
    public CategoryResponse update(Long userId, Long id, CategoryRequest request) {
        requireUser(userId);

        CategoryModel category = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada: " + id));

        var type = typeRepository.findById(request.typeId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Tipo de transação não encontrado: " + request.typeId()));

        if (repository.hasTransactionsWithDifferentType(id, request.typeId())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A categoria possui transações com outro tipo");
        }
        mapper.updateEntity(request, category, type);

        return mapper.response(repository.save(category));
    }

    @Transactional
    public void delete(Long userId, Long id) {
        requireUser(userId);
        CategoryModel category = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada: " + id));
        repository.delete(category);
    }

    private void requireUser(Long userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado: " + userId);
        }
    }
}
