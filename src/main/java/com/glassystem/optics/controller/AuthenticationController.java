package com.glassystem.optics.controller;


import java.text.ParseException;

import com.glassystem.optics.dto.request.AuthenticationRequest;
import com.glassystem.optics.dto.request.IntrospectRequest;
import com.glassystem.optics.dto.request.LogoutRequest;
import com.glassystem.optics.dto.request.RefreshRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.AuthenticationResponse;
import com.glassystem.optics.dto.response.IntrospectResponse;
import com.glassystem.optics.service.AuthenticationService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;


import com.nimbusds.jose.JOSEException;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuthenticationController {
    AuthenticationService authenticationService;

    @PostMapping("/login")
    ApiResponse<AuthenticationResponse> authenticate(@RequestBody AuthenticationRequest authenticationRequest) {
        var result = authenticationService.authenticate(authenticationRequest);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/check-token")
    ApiResponse<IntrospectResponse> authenticate(@RequestBody IntrospectRequest introspectRequest)
            throws ParseException, JOSEException {
        var result = authenticationService.introspect(introspectRequest);
        return ApiResponse.<IntrospectResponse>builder().result(result).build();
    }

    @PostMapping("/refresh-token")
    ApiResponse<AuthenticationResponse> refresh(@RequestBody RefreshRequest refreshRequest)
            throws ParseException, JOSEException {
        var result = authenticationService.refreshToken(refreshRequest);
        return ApiResponse.<AuthenticationResponse>builder().result(result).build();
    }

    @PostMapping("/logout")
    ApiResponse<Void> logout(@RequestBody LogoutRequest logoutRequest) throws ParseException, JOSEException {
        authenticationService.logout(logoutRequest);
        return ApiResponse.<Void>builder().build();
    }
}