package com.cashflow.demo.repository;

import com.cashflow.demo.entity.CategoryModel;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<CategoryModel, Long> {
    Optional<CategoryModel> findByIdAndUser_Id(Long id, Long userId);

    List<CategoryModel> findAllByUser_Id(Long userId);

    List<CategoryModel> findByUser_IdAndTitleContainingIgnoreCase(Long userId, String title);

}
