package com.cashflow.demo.repository;

import com.cashflow.demo.entity.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface TransactionRepository extends JpaRepository<TransactionModel, Long>, JpaSpecificationExecutor<TransactionModel> {
    Page<TransactionModel> findAllByUser_Id(Long userId, Pageable pageable);

    Optional<TransactionModel> findByIdAndUser_Id(Long id, Long userId);
}
