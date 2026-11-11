package com.cashflow.demo.mapper;

import com.cashflow.demo.dto.category.CategoryRequest;
import com.cashflow.demo.dto.category.CategoryResponse;
import com.cashflow.demo.entity.CategoryModel;
import com.cashflow.demo.entity.TransactionTypeModel;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapper {

    private final TransactionTypeMapper typeMapper;

    public CategoryMapper(TransactionTypeMapper typeMapper) {
        this.typeMapper = typeMapper;
    }


    public CategoryModel toEntity(CategoryRequest request, TransactionTypeModel type) {
        CategoryModel category = new CategoryModel();
        updateEntity(request, category, type);
        return category;
    }

    public void updateEntity(CategoryRequest request, CategoryModel category, TransactionTypeModel type) {
        category.setTitle(request.title());
        category.setType(type);
    }

    public CategoryResponse response(CategoryModel category) {
        return new CategoryResponse(
                category.getId(),
                category.getTitle(),
                typeMapper.response(category.getType())
        );
    }

}
