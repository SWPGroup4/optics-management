package com.glassystem.optics.repository;


import com.glassystem.optics.entity.InvalidatedToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;


@Repository
public interface InvalidatedTokenRepository extends JpaRepository<InvalidatedToken, String> {}
