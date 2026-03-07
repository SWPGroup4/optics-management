package com.glassystem.optics.controller;

import com.glassystem.optics.dto.request.LensCreateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.LensResponse;
import com.glassystem.optics.service.LensService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/lenses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LensController {
    LensService lensService;

    @PostMapping
    @PreAuthorize("hasRole('OPERATION') or hasRole('ADMIN')")
    ApiResponse<LensResponse> createLens(@RequestBody @Valid LensCreateRequest request) {
        return ApiResponse.<LensResponse>builder()
                .result(lensService.createLens(request))
                .message("Lens created successfully")
                .build();
    }
}
