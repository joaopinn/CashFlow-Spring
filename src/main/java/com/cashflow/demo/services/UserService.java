package com.cashflow.demo.services;

import com.cashflow.demo.dto.user.UserRequest;
import com.cashflow.demo.dto.user.UserResponse;
import com.cashflow.demo.entity.UserModel;
import com.cashflow.demo.mapper.UserMapper;
import com.cashflow.demo.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    public UserService(UserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public List<UserResponse> findAll() {
        return repository.findAll().stream().map(mapper::response).toList();
    }

    public UserResponse findById(Long id) {
        return mapper.response(findUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        return mapper.response(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        UserModel user = findUser(id);
        mapper.updateEntity(request, user);
        return mapper.response(repository.save(user));
    }

    @Transactional
    public void delete(Long id) {
        repository.delete(findUser(id));
    }

    private UserModel findUser(Long id) {
        return repository.findById(id).orElseThrow(() -> new ResponseStatusException(
                HttpStatus.NOT_FOUND, "Usuário não encontrado com o ID: " + id));
    }
}
