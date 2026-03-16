package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface NotificationRepository extends JpaRepository<Notification, String> {
    List<Notification> findByRecipientIdOrderByCreatedAtDesc(String recipientId);

    Optional<Notification> findByIdAndRecipientId(String id, String recipientId);

    long countByRecipientIdAndIsReadFalse(String recipientId);
}