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
}
