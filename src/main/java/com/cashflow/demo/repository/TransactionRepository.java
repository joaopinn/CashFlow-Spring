package com.cashflow.demo.repository;

import com.cashflow.demo.entity.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.ScopedValue;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionModel, Long> {
    List<TransactionModel> findAllByUser_Id(Long userId);

    List<TransactionModel> findByUser_IdAndTitleContainingIgnoreCase(
            Long userId, String name);

    Optional<TransactionModel> findByIdAndUser_Id(Long id, Long userId);
}
