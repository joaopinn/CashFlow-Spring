package com.cashflow.demo.mapper;

import com.cashflow.demo.dto.transaction.TransactionRequest;
import com.cashflow.demo.dto.transaction.TransactionResponse;
import com.cashflow.demo.entity.TransactionModel;
import com.cashflow.demo.entity.TransactionTypeModel;
import com.cashflow.demo.entity.CategoryModel;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    private final TransactionTypeMapper typeMapper;
    private final CategoryMapper categoryMapper;

    public TransactionMapper(TransactionTypeMapper typeMapper, CategoryMapper categoryMapper) {
        this.typeMapper = typeMapper;
        this.categoryMapper = categoryMapper;
    }


    public TransactionModel toEntity(TransactionRequest request, TransactionTypeModel type, CategoryModel category) {
        TransactionModel transaction = new TransactionModel();
        updateEntity(request, transaction, type, category);
        return transaction;
    }

    public void updateEntity(TransactionRequest request, TransactionModel transaction, TransactionTypeModel type, CategoryModel category) {
        transaction.setDescription(request.description());
        transaction.setAmount(request.amount());
        transaction.setType(type);
        transaction.setCategory(category);
        transaction.setDate(request.date());
        transaction.setStatus(request.status());
    }

    public TransactionResponse response(TransactionModel transaction) {
        return new TransactionResponse(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                typeMapper.response(transaction.getType()),
                categoryMapper.response(transaction.getCategory()),
                transaction.getDate(),
                transaction.getStatus()
        );
    }
}
