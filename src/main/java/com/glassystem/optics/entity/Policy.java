package com.glassystem.optics.entity;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "policy")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Policy {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	Integer id;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "manager_user_id", nullable = false)
	User managerUser;

	@Column(nullable = false, unique = true)
	String code;

	@Column(nullable = false)
	String title;

	@Column(columnDefinition = "TEXT")
	String description;

	@Column(name = "effective_from")
	LocalDate effectiveFrom;

	@Column(name = "effective_to")
	LocalDate effectiveTo;

	@Column(name = "created_at", updatable = false)
	LocalDateTime createdAt;

	@PrePersist
	protected void onCreate() {
		createdAt = LocalDateTime.now();
	}
}
