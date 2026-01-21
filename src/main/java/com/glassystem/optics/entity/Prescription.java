package com.glassystem.optics.entity;


import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "prescriptions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Prescription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    // Mắt phải (OD - Right Eye)
    Double odSphere;
    Double odCylinder;
    Integer odAxis;
    Double odAdd;
    Double odPd;

    // Mắt trái (OS - Left Eye)
    Double osSphere;
    Double osCylinder;
    Integer osAxis;
    Double osAdd;
    Double osPd;

    String note; // Ghi chú thêm nếu cần

}
