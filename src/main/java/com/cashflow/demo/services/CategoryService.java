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

import java.util.List;

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

    public List<CategoryResponse> findAll(Long userId) {
        requireUser(userId);
        return repository.findAllByUser_Id(userId).stream().map(mapper::response).toList();
    }

    public CategoryResponse findById(Long userId, Long id) {
        requireUser(userId);
        CategoryModel category = repository.findByIdAndUser_Id(id, userId).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Categoria não encontrada: " + id));
        return mapper.response(category);
    }

    public List<CategoryResponse> findByName(Long userId,
                                             String name) {
        requireUser(userId);

        if (name == null || name.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe o nome da categoria");
        }

        return repository.findByUser_IdAndTitleContainingIgnoreCase(userId, name.strip())
                .stream().map(mapper::response).toList();
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
