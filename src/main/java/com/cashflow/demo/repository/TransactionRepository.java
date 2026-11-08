package com.cashflow.demo.repository;

import com.cashflow.demo.entity.TransactionModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionRepository extends JpaRepository<TransactionModel, Long> {}
