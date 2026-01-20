package com.glassystem.optics.service;

import java.io.IOException;
import java.time.LocalDate;
import java.time.Period;
import java.util.HashSet;
import java.util.List;

import com.glassystem.optics.constant.PredefinedRole;
import com.glassystem.optics.dto.request.AdminUserUpdateRequest;
import com.glassystem.optics.dto.request.UserCreationRequest;
import com.glassystem.optics.dto.request.UserUpdateRequest;
import com.glassystem.optics.dto.response.UserResponse;
import com.glassystem.optics.entity.Role;
import com.glassystem.optics.entity.User;
import com.glassystem.optics.enums.S3ImageName;
import com.glassystem.optics.enums.UserStatus;
import com.glassystem.optics.exception.AppException;
import com.glassystem.optics.exception.ErrorCode;
import com.glassystem.optics.mapper.UserMapper;
import com.glassystem.optics.repository.RoleRepository;
import com.glassystem.optics.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.access.prepost.PostAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class UserService {

    UserRepository userRepository;
    RoleRepository roleRepository;
    UserMapper userMapper;
    PasswordEncoder passwordEncoder;
    FileStorageService fileStorageService;

    public UserResponse createUser(UserCreationRequest request, MultipartFile avatarFile) {
        log.info("User creation request");

        if (userRepository.existsByUsername(request.getUsername())) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }
        User user = userMapper.toUser(request);
        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String avatarUrl = fileStorageService.uploadFile(avatarFile, S3ImageName.AVATAR);
                user.setImageUrl(avatarUrl);
            } catch (IOException e) {
                throw new AppException(ErrorCode.CANNOT_UPLOAD_IMAGE);
            }
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setPassword(passwordEncoder.encode(user.getPassword()));

        HashSet<Role> roles = new HashSet<>();
        roleRepository.findById(PredefinedRole.CUSTOMER_ROLE).ifPresent(roles::add);
        user.setRoles(roles);

        try {
            user = userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.USER_EXISTED);
        }

        return userMapper.toUserResponse(user);
    }

    public UserResponse getMyInfo() {
        var context = SecurityContextHolder.getContext();
        String name = context.getAuthentication().getName();

        User user = userRepository.findByUsername(name).orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        return userMapper.toUserResponse(user);
    }

    // @PreAuthorize("hasRole('ADMIN')")
    // @PostAuthorize("hasAuthority('APPROVE_POST')")
    public List<UserResponse> getUsers() {
        log.info("In method getUsers");
        return userRepository.findAll().stream().map(userMapper::toUserResponse).toList();
    }

    public UserResponse getUser(String id) {
        log.info("In method getUser by id");
        return userMapper.toUserResponse(
                userRepository.findById(id).orElseThrow(() -> new RuntimeException("User not found")));
    }

    public UserResponse updateMyProfile(UserUpdateRequest request, MultipartFile avatarFile) {

        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_EXISTED));

        String oldAvatarUrl = user.getImageUrl();

        userMapper.updateUser(user, request);

        if (request.getPassword() != null && !request.getPassword().isBlank()) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                String newAvatarUrl = fileStorageService.uploadFile(avatarFile, S3ImageName.AVATAR);
                user.setImageUrl(newAvatarUrl);
            } catch (IOException e) {
                throw new AppException(ErrorCode.CANNOT_UPLOAD_IMAGE);
            }
        }

        userRepository.save(user);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            fileStorageService.deleteFileByUrl(oldAvatarUrl);
        }

        return userMapper.toUserResponse(user);
    }

    public UserResponse updateUserByAdmin(String id, AdminUserUpdateRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException(ErrorCode.USER_NOT_EXISTED.getMessage()));
        userMapper.updateUserByAdmin(user, request);
        if (request.getPassword() != null) {
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }

        if (request.getRoles() != null) {
            var roles = roleRepository.findAllById(request.getRoles());
            user.setRoles(new HashSet<>(roles));
        }
        if (request.getStatus() != null) {
            user.setStatus(request.getStatus());
        }
        return userMapper.toUserResponse(userRepository.save(user));
    }

    public void deleteUser(String id) {
        userRepository.deleteById(id);
    }
}
