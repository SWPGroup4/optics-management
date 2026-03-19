package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FeedbackRepository extends JpaRepository<Feedback, String> {
    List<Feedback> findByProductId(String productId);

    List<Feedback> findByCustomerId(String customerId);

    List<Feedback> findByOrderId(String orderId);

    Optional<Feedback> findByOrderIdAndProductIdAndCustomerId(String orderId, String productId, String customerId);

    boolean existsByOrderIdAndProductIdAndCustomerId(String orderId, String productId, String customerId);
}
