package com.glassystem.optics.repository;


import com.glassystem.optics.entity.Prescription;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PrescriptionRepository extends JpaRepository<Prescription,String> {

}
