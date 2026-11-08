package com.cashflow.demo.repository;

import com.cashflow.demo.entity.TransactionTypeModel;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TransactionTypeRepository extends JpaRepository<TransactionTypeModel, Long> {}
