package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.PrescriptionRequest;
import com.glassystem.optics.entity.Prescription;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {
    Prescription toPrescription(PrescriptionRequest prescriptionRequest);
}
