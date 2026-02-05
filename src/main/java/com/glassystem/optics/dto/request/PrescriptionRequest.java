package com.glassystem.optics.dto.request;


import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import lombok.experimental.FieldDefaults;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionRequest {
    @Schema(hidden = true)
    String imageUrl;
    Double odSphere; Double odCylinder; Integer odAxis; Double odAdd; Double odPd;
    Double osSphere; Double osCylinder; Integer osAxis; Double osAdd; Double osPd;
    String note;
}
