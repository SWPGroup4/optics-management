package com.glassystem.optics.controller;

import java.io.IOException;
import java.util.List;

import com.glassystem.optics.dto.request.AdminUserUpdateRequest;
import com.glassystem.optics.dto.request.UserCreationRequest;
import com.glassystem.optics.dto.request.UserUpdateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.UserResponse;
import com.glassystem.optics.service.UserService;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;

import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
    UserService userService;

    @PostMapping(value = "/registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    ApiResponse<UserResponse> createUser(@RequestPart("data") @Valid UserCreationRequest request,
                                         @RequestPart(value = "imageUrl", required = false) MultipartFile imageUrl) {
        log.info("controller: create user");
        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request, imageUrl));

        return apiResponse;
    }

    @GetMapping()
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<List<UserResponse>> getUsers() {

        var authentication = SecurityContextHolder.getContext().getAuthentication();

        log.info("Username is {}", authentication.getName());
        authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

        return ApiResponse.<List<UserResponse>>builder()
                .result(userService.getUsers())
                .build();
    }

    @GetMapping("/me")
    ApiResponse<UserResponse> getMyInfo() {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getMyInfo())
                .build();
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<UserResponse> getUserById(@PathVariable("userId") String userId) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.getUser(userId))
                .build();
    }

    @PutMapping(value = "/me")
    @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<UserResponse> updateMyProfile(@RequestBody @Valid UserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyProfile(request, null))
                .build();
    }

    @PutMapping(value = "/me/with-avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<UserResponse> updateMyProfileWithAvatar(@RequestPart("data") @Valid UserUpdateRequest request,
            @Parameter(schema = @Schema(type = "string", format = "binary"))
            @RequestPart(value = "imageUrl", required = false) MultipartFile imageUrl) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyProfile(request, imageUrl))
                .build();
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<UserResponse> updateUser(@PathVariable("id") String userId,
            @RequestBody @Valid AdminUserUpdateRequest request) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateUserByAdmin(userId, request))
                .build();
    }

    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
        userService.deleteUser(userId);
        return ApiResponse.<String>builder()
                .result("User has been deleted successfully")
                .build();
    }
}
