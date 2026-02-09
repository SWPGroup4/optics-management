package com.glassystem.optics.dto.response;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionResponse {
    String id;

    String imageUrl;

    // Mắt phải (OD)
    Double odSphere;
    Double odCylinder;
    Integer odAxis;
    Double odAdd;
    Double odPd;

    // Mắt trái (OS)
    Double osSphere;
    Double osCylinder;
    Integer osAxis;
    Double osAdd;
    Double osPd;

    String note;
}