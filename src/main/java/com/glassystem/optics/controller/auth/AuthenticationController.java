package com.glassystem.optics.controller.auth;

import java.text.ParseException;

import com.glassystem.optics.dto.request.AuthenticationRequest;
import com.glassystem.optics.dto.request.IntrospectRequest;
import com.glassystem.optics.dto.request.LogoutRequest;
import com.glassystem.optics.dto.request.RefreshRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.AuthenticationResponse;
import com.glassystem.optics.dto.response.IntrospectResponse;
import com.glassystem.optics.service.AuthenticationService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Tag(name = "Authentication Management", description = "Endpoints for user identity, session, and token management")
public class AuthenticationController {

    AuthenticationService authenticationService;

    @PostMapping("/login")
    @Operation(summary = "Login to the system", description = "Returns an Access Token and Refresh Token upon successful credentials verification")
    public ApiResponse<AuthenticationResponse> authenticate(@RequestBody @Valid AuthenticationRequest request) {
        var result = authenticationService.authenticate(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/introspect")
    @Operation(summary = "Introspect Token", description = "Checks the validity and active status of a provided token")
    public ApiResponse<IntrospectResponse> introspect(@RequestBody @Valid IntrospectRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(request);
        return ApiResponse.<IntrospectResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/refresh")
    @Operation(summary = "Refresh Access Token", description = "Exchange a valid Refresh Token for a new Access Token")
    public ApiResponse<AuthenticationResponse> refresh(@RequestBody @Valid RefreshRequest request)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(request);
        return ApiResponse.<AuthenticationResponse>builder()
                .result(result)
                .build();
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout user", description = "Invalidates the current session and token to prevent further access")
    public ApiResponse<Void> logout(@RequestBody @Valid LogoutRequest request)
            throws ParseException, JOSEException {
        authenticationService.logout(request);
        return ApiResponse.<Void>builder()
                .message("User logged out successfully")
                .build();
    }
}