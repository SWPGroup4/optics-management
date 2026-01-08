package com.glassystem.optics.repository;


import java.util.Optional;

import com.glassystem.optics.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface UserRepository extends JpaRepository<User, String> {
    boolean existsByUsername(String username);

    Optional<User> findByUsername(String username);
}
