package com.glassystem.optics.repository;


import com.glassystem.optics.entity.Refund;
import com.glassystem.optics.enums.RefundStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RefundRepository extends JpaRepository<Refund,String> {

    List<Refund> findByStatus(RefundStatus status);

}