package com.cashflow.demo.mapper;

import com.cashflow.demo.dto.transactiontype.TransactionTypeRequest;
import com.cashflow.demo.dto.transactiontype.TransactionTypeResponse;
import com.cashflow.demo.entity.TransactionTypeModel;
import org.springframework.stereotype.Component;

@Component
public class TransactionTypeMapper {

    public TransactionTypeModel toEntity(TransactionTypeRequest request) {
        TransactionTypeModel type = new TransactionTypeModel();
        updateEntity(request, type);
        return type;
    }

    public void updateEntity(TransactionTypeRequest request, TransactionTypeModel type) {
        type.setTitle(request.title());
    }

    public TransactionTypeResponse response(TransactionTypeModel type) {
        return new TransactionTypeResponse(
                type.getId(),
                type.getTitle()
        );
    }
}
