package com.glassystem.optics.repository;


import com.glassystem.optics.dto.response.RefundResponse;
import com.glassystem.optics.entity.Orders;
import com.glassystem.optics.entity.Refund;
import com.glassystem.optics.enums.RefundStatus;
import org.mapstruct.Mapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RefundRepository extends JpaRepository<Refund,String> {

    List<Refund> findByStatus(RefundStatus status);
    Boolean existsByOrderId(String orderId);




}