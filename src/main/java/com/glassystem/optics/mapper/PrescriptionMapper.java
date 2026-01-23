package com.glassystem.optics.mapper;


import com.glassystem.optics.dto.request.PrescriptionRequest;
import com.glassystem.optics.entity.Prescription;
import org.mapstruct.Mapper;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface PrescriptionMapper {
    Prescription toPrescription(PrescriptionRequest prescriptionRequest);

    void updatePrescription(@MappingTarget Prescription prescription, PrescriptionRequest prescriptionRequest);
}
