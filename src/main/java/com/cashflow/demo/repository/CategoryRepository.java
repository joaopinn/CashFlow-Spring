package com.cashflow.demo.repository;

import com.cashflow.demo.entity.CategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {
}
