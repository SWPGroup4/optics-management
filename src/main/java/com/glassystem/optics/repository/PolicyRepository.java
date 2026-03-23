package com.glassystem.optics.repository;

import com.glassystem.optics.entity.Policy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface PolicyRepository extends
		JpaRepository<Policy, Integer>,
		JpaSpecificationExecutor<Policy> {
   // crud căn bản , tìm kiếm by id findById findByname
	boolean existsByCode(String code);

	boolean existsByCodeAndIdNot(String code, Integer id);
}
