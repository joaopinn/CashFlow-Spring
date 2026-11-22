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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import static com.cashflow.demo.repository.specification.TextFilters.containsIgnoreCase;

@Service
@Transactional(readOnly = true)
public class UserService {
    private final UserRepository repository;
    private final UserMapper mapper;

    public UserService(UserRepository repository, UserMapper mapper) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Page<UserResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(mapper::response);
    }

    public Page<UserResponse> search(String name, String email, String segment, String role,
                                     Pageable pageable) {
        return repository.findAll((root, query, cb) -> {
            var filters = new ArrayList<Predicate>();
            if (name != null && !name.isBlank()) {
                filters.add(containsIgnoreCase(cb, root.get("name"), name));
            }
            if (email != null && !email.isBlank()) {
                filters.add(containsIgnoreCase(cb, root.get("email"), email));
            }
            if (segment != null && !segment.isBlank()) {
                filters.add(containsIgnoreCase(cb, root.get("segment"), segment));
            }
            if (role != null && !role.isBlank()) {
                filters.add(cb.equal(cb.lower(root.get("role")), role.strip().toLowerCase(java.util.Locale.ROOT)));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        }, pageable).map(mapper::response);
    }

    public UserResponse findById(Long id) {
        return mapper.response(findUser(id));
    }

    @Transactional
    public UserResponse create(UserRequest request) {
        if (repository.existsByEmailIgnoreCase(request.email().strip())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        return mapper.response(repository.save(mapper.toEntity(request)));
    }

    @Transactional
    public UserResponse update(Long id, UserRequest request) {
        UserModel user = findUser(id);
        if (repository.existsByEmailIgnoreCaseAndIdNot(request.email().strip(), id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "E-mail já cadastrado");
        }
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        boolean admin = authentication != null && authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        if (!java.util.Objects.equals(request.role(), user.getRole()) && !admin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Somente administradores podem alterar o perfil");
        }
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
