package com.cashflow.demo.services;

import com.cashflow.demo.dto.category.CategoryRequest;
import com.cashflow.demo.entity.CategoryModel;
import com.cashflow.demo.entity.TransactionTypeModel;
import com.cashflow.demo.entity.UserModel;
import com.cashflow.demo.mapper.CategoryMapper;
import com.cashflow.demo.mapper.TransactionTypeMapper;
import com.cashflow.demo.repository.CategoryRepository;
import com.cashflow.demo.repository.TransactionTypeRepository;
import com.cashflow.demo.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CategoryServiceTest {
    private final CategoryRepository repository = mock(CategoryRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final TransactionTypeRepository types = mock(TransactionTypeRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final UserRepository users = mock(UserRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final CategoryService service = new CategoryService(repository,
            new CategoryMapper(new TransactionTypeMapper()), types, users);

    @Test
    void findsCategoryByIdWithinUser() {
        var category = new CategoryModel();
        category.setId(10L);
        category.setTitle("Alimentação");
        var type = new TransactionTypeModel();
        type.setId(2L);
        category.setType(type);
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(category));

        var response = service.findById(1L, 10L);

        assertEquals(10L, response.id());
        assertEquals("Alimentação", response.title());
        verify(repository).findByIdAndUser_Id(10L, 1L);
    }

    @Test
    void findByIdRejectsMissingCategoryOrDifferentOwner() {
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class, () -> service.findById(1L, 10L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
    }

    @Test
    void findByIdRejectsMissingUser() {
        when(users.existsById(1L)).thenReturn(false);

        var error = assertThrows(ResponseStatusException.class, () -> service.findById(1L, 10L));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void updatesExistingCategoryPreservingIdAndOwner() {
        var owner = new UserModel();
        var category = new CategoryModel();
        category.setId(10L);
        category.setUser(owner);
        category.setTitle("Antiga");
        var type = new TransactionTypeModel();
        type.setId(2L);
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(category));
        when(types.findById(2L)).thenReturn(Optional.of(type));
        when(repository.save(category)).thenReturn(category);

        var response = service.update(1L, 10L, new CategoryRequest("Nova", 2L));

        assertEquals(10L, response.id());
        assertEquals("Nova", response.title());
        assertSame(owner, category.getUser());
        assertSame(type, category.getType());
        verify(repository).save(same(category));
    }

    @Test
    void rejectsCategoryMissingOrOwnedByAnotherUser() {
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, new CategoryRequest("Nova", 2L)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(types);
    }

    @Test
    void rejectsMissingTypeWithoutChangingCategory() {
        var category = new CategoryModel();
        category.setTitle("Antiga");
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(category));
        when(types.findById(2L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, new CategoryRequest("Nova", 2L)));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Antiga", category.getTitle());
        verify(repository, never()).save(any());
    }
    @Test
    void searchRejectsInvalidTypeId() {
        when(users.existsById(1L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.search(1L, null, 0L,
                        org.springframework.data.domain.PageRequest.of(0, 20)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void searchAlwaysScopesResultsToUserAndCombinesFilters() {
        var pageable = org.springframework.data.domain.PageRequest.of(1, 5);
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<CategoryModel>>any(),
                eq(pageable))).thenAnswer(invocation -> {
            org.springframework.data.jpa.domain.Specification<CategoryModel> specification = invocation.getArgument(0);
            jakarta.persistence.criteria.Root<CategoryModel> root = mock(jakarta.persistence.criteria.Root.class);
            jakarta.persistence.criteria.Path<Object> user = mock(jakarta.persistence.criteria.Path.class);
            jakarta.persistence.criteria.Path<Object> userId = mock(jakarta.persistence.criteria.Path.class);
            jakarta.persistence.criteria.Path<Object> type = mock(jakarta.persistence.criteria.Path.class);
            jakarta.persistence.criteria.Path<Object> typeId = mock(jakarta.persistence.criteria.Path.class);
            jakarta.persistence.criteria.Path<String> title = mock(jakarta.persistence.criteria.Path.class);
            var cb = mock(jakarta.persistence.criteria.CriteriaBuilder.class);
            var lowered = mock(jakarta.persistence.criteria.Expression.class);
            when(root.get("user")).thenReturn(user);
            when(user.get("id")).thenReturn(userId);
            when(root.get("type")).thenReturn(type);
            when(type.get("id")).thenReturn(typeId);
            when(root.<String>get("title")).thenReturn(title);
            when(cb.lower(title)).thenReturn(lowered);

            specification.toPredicate(root, null, cb);

            verify(cb).equal(userId, 1L);
            verify(cb).equal(typeId, 2L);
            verify(cb).like(lowered, "%alimentação%", '!');
            return org.springframework.data.domain.Page.empty(pageable);
        });

        var result = service.search(1L, " Alimentação ", 2L, pageable);
        assertEquals(1, result.getNumber());
        assertEquals(5, result.getSize());
        verify(repository).findAll(org.mockito.ArgumentMatchers.<org.springframework.data.jpa.domain.Specification<CategoryModel>>any(), eq(pageable));
    }

}
