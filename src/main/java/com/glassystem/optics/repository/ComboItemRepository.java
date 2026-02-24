package com.glassystem.optics.repository;

import com.glassystem.optics.entity.ComboItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository cho entity ComboItem.
 */
@Repository
public interface ComboItemRepository extends JpaRepository<ComboItem, String> {

	/** Lấy tất cả item của một combo */
	List<ComboItem> findAllByCombo_Id(String comboId);

	/** Xóa tất cả item của một combo (dùng khi update combo - replace items) */
	void deleteAllByCombo_Id(String comboId);
}
