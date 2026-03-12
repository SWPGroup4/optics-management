package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Inventory;
import com.glassystem.optics.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, String> {
    List<Payment> findByOrderId(String orderId);
    Optional<Payment> findFirstByOrderId(String orderId);

}
