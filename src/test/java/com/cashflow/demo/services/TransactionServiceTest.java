package com.cashflow.demo.services;

import com.cashflow.demo.dto.transaction.TransactionRequest;
import com.cashflow.demo.entity.CategoryModel;
import com.cashflow.demo.entity.TransactionModel;
import com.cashflow.demo.entity.TransactionTypeModel;
import com.cashflow.demo.entity.UserModel;
import com.cashflow.demo.mapper.CategoryMapper;
import com.cashflow.demo.mapper.TransactionMapper;
import com.cashflow.demo.mapper.TransactionTypeMapper;
import com.cashflow.demo.repository.*;
import org.junit.jupiter.api.Test;
import org.mockito.MockMakers;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class TransactionServiceTest {
    private final TransactionRepository repository = mock(TransactionRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final UserRepository users = mock(UserRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final TransactionTypeRepository types = mock(TransactionTypeRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final CategoryRepository categories = mock(CategoryRepository.class,
            withSettings().mockMaker(MockMakers.PROXY));
    private final TransactionTypeMapper typeMapper = new TransactionTypeMapper();
    private final TransactionService service = new TransactionService(repository, users,
            new TransactionMapper(typeMapper, new CategoryMapper(typeMapper)), types, categories);
    private final TransactionRequest request = new TransactionRequest("Mercado",
            new BigDecimal("125.50"), 2L, 3L, LocalDate.of(2026, 11, 11), "PAGO");

    @Test
    void updatesExistingTransactionPreservingIdAndOwner() {
        var owner = new UserModel();
        var transaction = new TransactionModel();
        transaction.setId(10L);
        transaction.setUser(owner);
        var type = new TransactionTypeModel();
        type.setId(2L);
        var category = new CategoryModel();
        category.setId(3L);
        category.setType(type);
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(transaction));
        when(types.findById(2L)).thenReturn(Optional.of(type));
        when(categories.findByIdAndUser_Id(3L, 1L)).thenReturn(Optional.of(category));
        when(repository.save(transaction)).thenReturn(transaction);

        var response = service.update(1L, 10L, request);

        assertEquals(10L, response.id());
        assertEquals(request.description(), response.description());
        assertEquals(request.amount(), response.amount());
        assertEquals(request.date(), response.date());
        assertEquals(request.status(), response.status());
        assertSame(owner, transaction.getUser());
        assertSame(type, transaction.getType());
        assertSame(category, transaction.getCategory());
        verify(repository).save(same(transaction));
    }

    @Test
    void rejectsMissingTransactionOrDifferentOwner() {
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, request));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        verify(repository, never()).save(any());
        verifyNoInteractions(types, categories);
    }

    @Test
    void rejectsCategoryFromAnotherUserWithoutChangingTransaction() {
        var transaction = new TransactionModel();
        transaction.setDescription("Original");
        when(users.existsById(1L)).thenReturn(true);
        when(repository.findByIdAndUser_Id(10L, 1L)).thenReturn(Optional.of(transaction));
        when(types.findById(2L)).thenReturn(Optional.of(new TransactionTypeModel()));
        when(categories.findByIdAndUser_Id(3L, 1L)).thenReturn(Optional.empty());

        var error = assertThrows(ResponseStatusException.class,
                () -> service.update(1L, 10L, request));

        assertEquals(HttpStatus.NOT_FOUND, error.getStatusCode());
        assertEquals("Original", transaction.getDescription());
        verify(repository, never()).save(any());
    }
    @Test
    void rejectsInvertedSearchDateRange() {
        when(users.existsById(1L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.search(1L, null, null, LocalDate.of(2026, 11, 12),
                        LocalDate.of(2026, 11, 1), org.springframework.data.domain.PageRequest.of(0, 20)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

    @Test
    void rejectsInvalidSearchTypeId() {
        when(users.existsById(1L)).thenReturn(true);
        var error = assertThrows(ResponseStatusException.class,
                () -> service.search(1L, null, 0L, null, null,
                        org.springframework.data.domain.PageRequest.of(0, 20)));
        assertEquals(HttpStatus.BAD_REQUEST, error.getStatusCode());
        verifyNoInteractions(repository);
    }

}
