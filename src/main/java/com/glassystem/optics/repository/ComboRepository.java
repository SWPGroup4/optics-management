package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Combo;
import com.glassystem.optics.enums.ComboStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository cho entity Combo.
 *
 * Cung cấp các query phục vụ:
 * - Lọc combo theo keyword, status, khoảng thời gian (admin view)
 * - Lấy combo khả dụng (ACTIVE + trong thời gian hiệu lực)
 * - Lấy combo chưa bị disable thủ công (cho background job sync status)
 */

@Repository
public interface ComboRepository extends
		JpaRepository<Combo, String>,
		JpaSpecificationExecutor<Combo> {

	@Query("SELECT c FROM Combo c WHERE c.status = 'ACTIVE' " +
			"AND c.startTime <= :currentTime AND c.endTime >= :currentTime")
	List<Combo> findAvailableCombos(@Param("currentTime") LocalDateTime currentTime);

	List<Combo> findAllByIsManuallyDisabledFalse();
}
