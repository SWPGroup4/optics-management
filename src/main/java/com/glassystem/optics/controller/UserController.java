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
    ApiResponse<UserResponse> createUser(@RequestPart("data")
                                         @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                 content = @io.swagger.v3.oas.annotations.media.Content(
                                                         encoding = @io.swagger.v3.oas.annotations.media.Encoding(name = "data", contentType = "application/json")
                                                 )
                                         )
                                         @Valid UserCreationRequest request,
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


    @PatchMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('CUSTOMER')")
    ApiResponse<UserResponse> updateMyProfile(@RequestPart("data")
                                              @io.swagger.v3.oas.annotations.parameters.RequestBody(
                                                      content = @io.swagger.v3.oas.annotations.media.Content(
                                                              encoding = @io.swagger.v3.oas.annotations.media.Encoding(name = "data", contentType = "application/json")
                                                      )
                                              )
                                              @Valid UserUpdateRequest request,
            @RequestPart(value = "imageUrl", required = false) MultipartFile imageUrl) {
        return ApiResponse.<UserResponse>builder()
                .result(userService.updateMyProfile(request, imageUrl))
                .build();
    }

    @PatchMapping("/{id}")
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
