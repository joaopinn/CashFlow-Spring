package com.cashflow.demo.repository;

import com.cashflow.demo.entity.CategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long>, JpaSpecificationExecutor<CategoryModel> {
    Optional<CategoryModel> findByIdAndUser_Id(Long id, Long userId);

    Page<CategoryModel> findAllByUser_Id(Long userId, Pageable pageable);

    @Query("select (count(t) > 0) from TransactionModel t where t.category.id = :categoryId and t.type.id <> :typeId")
    boolean hasTransactionsWithDifferentType(Long categoryId, Long typeId);
}
