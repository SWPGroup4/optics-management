package com.glassystem.optics.controller.user;

import java.io.IOException;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.glassystem.optics.dto.request.AdminUserUpdateRequest;
import com.glassystem.optics.dto.request.UserCreationRequest;
import com.glassystem.optics.dto.request.UserUpdateRequest;
import com.glassystem.optics.dto.response.ApiResponse;
import com.glassystem.optics.dto.response.UserResponse;
import com.glassystem.optics.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
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
@Tag(name = "User Management", description = "Endpoints for user registration, account auditing, and profile management")
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserController {
        UserService userService;
        @Autowired
        ObjectMapper objectMapper;

    @PostMapping(value = "/registration", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Register a new user", description = "Standard registration for customers")
    ApiResponse<UserResponse> createUser(
            @RequestPart("data") String dataString,
            @RequestPart(value = "imageUrl", required = false) MultipartFile imageUrl) throws IOException {

        UserCreationRequest request = objectMapper.readValue(dataString, UserCreationRequest.class);

        ApiResponse<UserResponse> apiResponse = new ApiResponse<>();
        apiResponse.setResult(userService.createUser(request, imageUrl));
        return apiResponse;
    }

        @GetMapping()
        @Operation(summary = "Get all users", description = "Restricted to ADMIN. Retrieves a complete list of users in the system")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<List<UserResponse>> getUsers(@RequestParam(value = "role", required = false) String role) {
                var authentication = SecurityContextHolder.getContext().getAuthentication();
                authentication.getAuthorities().forEach(grantedAuthority -> log.info(grantedAuthority.getAuthority()));

                return ApiResponse.<List<UserResponse>>builder()
                                .result(userService.getUsers(role))
                                .build();
        }

        @GetMapping("/me")
        @Operation(summary = "Get current user profile", description = "Retrieves information about the currently authenticated user")
        ApiResponse<UserResponse> getMyInfo() {
                return ApiResponse.<UserResponse>builder()
                                .result(userService.getMyInfo())
                                .build();
        }

        @GetMapping("/{userId}")
        @Operation(summary = "Get user by ID", description = "Restricted to ADMIN. Fetches details of a specific user account")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<UserResponse> getUserById(@PathVariable("userId") String userId) {
                return ApiResponse.<UserResponse>builder()
                                .result(userService.getUser(userId))
                                .build();
        }

        @PutMapping(value = "/me", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
        @Operation(summary = "Update personal profile", description = "Allows a customer to update their own contact details and profile picture")
        @PreAuthorize("hasRole('CUSTOMER')")
        ApiResponse<UserResponse> updateMyProfile(
                        @RequestPart("data") @io.swagger.v3.oas.annotations.parameters.RequestBody(content = @io.swagger.v3.oas.annotations.media.Content(encoding = @io.swagger.v3.oas.annotations.media.Encoding(name = "data", contentType = "application/json"))) @Valid UserUpdateRequest request,
                        @RequestPart(value = "imageUrl", required = false) MultipartFile imageUrl) {
                return ApiResponse.<UserResponse>builder()
                                .result(userService.updateMyProfile(request, imageUrl))
                                .build();
        }

        @PutMapping("/{id}")
        @Operation(summary = "Update user as Admin", description = "Restricted to ADMIN. Allows modifying user status, roles")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<UserResponse> updateUser(@PathVariable("id") String userId,
                        @RequestBody @Valid AdminUserUpdateRequest request) {
                return ApiResponse.<UserResponse>builder()
                                .result(userService.updateUserByAdmin(userId, request))
                                .build();
        }

        @DeleteMapping("/{userId}")
        @Operation(summary = "Delete user account", description = "Restricted to ADMIN. Permanently removes a user from the system")
        @PreAuthorize("hasRole('ADMIN')")
        ApiResponse<String> deleteUser(@PathVariable("userId") String userId) {
                userService.deleteUser(userId);
                return ApiResponse.<String>builder()
                                .result("User has been deleted successfully")
                                .build();
        }
}
