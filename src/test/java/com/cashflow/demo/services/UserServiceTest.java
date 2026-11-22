package com.cashflow.demo.services;

import com.cashflow.demo.entity.UserModel;
import com.cashflow.demo.mapper.UserMapper;
import com.cashflow.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;

import java.util.List;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final UserService service = new UserService(repository, new UserMapper(null));

    @Test
    void listingPreservesPaginationAndMapsResponse() {
        var user = new UserModel();
        user.setId(7L);
        user.setName("João");
        var pageable = PageRequest.of(1, 2);
        when(repository.findAll(pageable)).thenReturn(new PageImpl<>(List.of(user), pageable, 3));

        var result = service.findAll(pageable);

        assertEquals(3, result.getTotalElements());
        assertEquals(1, result.getNumber());
        assertEquals(7L, result.getContent().getFirst().id());
        assertEquals("João", result.getContent().getFirst().name());
    }

    @Test
    void searchCombinesAllProvidedFilters() {
        var pageable = PageRequest.of(0, 20);
        when(repository.findAll(org.mockito.ArgumentMatchers.<Specification<UserModel>>any(), eq(pageable)))
                .thenAnswer(invocation -> {
                    Specification<UserModel> specification = invocation.getArgument(0);
                    jakarta.persistence.criteria.Root<UserModel> root = mock(jakarta.persistence.criteria.Root.class);
                    var cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
                    for (String field : List.of("name", "email", "segment", "role")) {
                        jakarta.persistence.criteria.Path<String> path = mock(jakarta.persistence.criteria.Path.class);
                        jakarta.persistence.criteria.Expression<String> lowered = mock(jakarta.persistence.criteria.Expression.class);
                        when(root.<String>get(field)).thenReturn(path);
                        when(cb.lower(path)).thenReturn(lowered);
                    }

                    specification.toPredicate(root, null, cb);

                    verify(cb).like(cb.lower(root.get("name")), "%joão%", '!');
                    verify(cb).like(cb.lower(root.get("email")), "%@empresa.com%", '!');
                    verify(cb).like(cb.lower(root.get("segment")), "%varejo%", '!');
                    verify(cb).equal(cb.lower(root.get("role")), "admin");
                    return org.springframework.data.domain.Page.empty(pageable);
                });

        assertTrue(service.search(" João ", "@empresa.com", "Varejo", " ADMIN ", pageable).isEmpty());
        verify(repository).findAll(org.mockito.ArgumentMatchers.<Specification<UserModel>>any(), eq(pageable));
    }
}
