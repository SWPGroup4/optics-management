package com.glassystem.optics.dto.request;


import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionRequest {
    String imageUrl;
    Double odSphere; Double odCylinder; Integer odAxis; Double odAdd; Double odPd;
    Double osSphere; Double osCylinder; Integer osAxis; Double osAdd; Double osPd;
    String note;
}
