package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Payment;
import com.glassystem.optics.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, String> {
    Optional<Transaction> findTopByPaymentIdOrderByDateTimeDesc(String paymentId);

}
