package com.glassystem.optics.repository;


import com.glassystem.optics.entity.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface PermissionRepository extends JpaRepository<Permission, String> {}
