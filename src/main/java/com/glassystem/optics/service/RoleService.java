package com.glassystem.optics.service;


import java.util.HashSet;
import java.util.List;

import com.glassystem.optics.dto.request.RoleRequest;
import com.glassystem.optics.dto.request.UserRoleUpdateRequest;
import com.glassystem.optics.dto.response.RoleResponse;
import com.glassystem.optics.dto.response.UserResponse;
import com.glassystem.optics.enums.UserRole;
import com.glassystem.optics.mapper.RoleMapper;
import com.glassystem.optics.mapper.UserMapper;
import com.glassystem.optics.repository.PermissionRepository;
import com.glassystem.optics.repository.RoleRepository;
import com.glassystem.optics.repository.UserRepository;
import org.springframework.stereotype.Service;



import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class RoleService {
    RoleRepository roleRepository;
    PermissionRepository permissionRepository;
    RoleMapper roleMapper;
    UserRepository userRepository;
    UserMapper userMapper;

    public RoleResponse create(RoleRequest roleRequest) {
        var role = roleMapper.toRole(roleRequest);

        var permissions = permissionRepository.findAllById(roleRequest.getPermissions());
        role.setPermissions(new HashSet<>(permissions));
        role = roleRepository.save(role);
        return roleMapper.toRoleResponse(role);
    }

    public List<RoleResponse> getAll() {
        return roleRepository.findAll().stream().map(roleMapper::toRoleResponse).toList();
    }

    public void delete(String role) {
        roleRepository.deleteById(role);
    }



    @Transactional
    public UserResponse changeUserRole(String userId, UserRole newRole) {
        var user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));
        var role = roleRepository.findById(newRole.name())
                .orElseThrow(() -> new RuntimeException("Role " + newRole.name() + " not found in system"));

        user.setRoles(new HashSet<>(List.of(role)));
        return userMapper.toUserResponse(userRepository.save(user));
    }
}
