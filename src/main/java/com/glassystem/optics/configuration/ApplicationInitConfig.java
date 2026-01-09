package com.glassystem.optics.configuration;


import java.util.HashSet;

import com.glassystem.optics.entity.Role;
import com.glassystem.optics.entity.User;
import com.glassystem.optics.repository.RoleRepository;
import com.glassystem.optics.repository.UserRepository;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Slf4j
public class ApplicationInitConfig {

    PasswordEncoder passwordEncoder;
    RoleRepository roleRepository;

    @Bean
    @ConditionalOnProperty(
            prefix = "spring",
            value = "datasource.driverClassName",
            havingValue = "org.postgresql.Driver")
    ApplicationRunner applicationRunner(UserRepository userRepository) {
        log.info("init ApplicationRunner...");
        return args -> {
            if (userRepository.findByUsername("admin").isEmpty()) {
                Role adminRole = roleRepository.save(
                        Role.builder().name("ADMIN").description("Admin role").build());

                var roles = new HashSet<Role>();
                roles.add(adminRole);

                User user = User.builder()
                        .username("admin")
                        .password(passwordEncoder.encode("admin"))
                        .roles(roles)
                        .build();

                userRepository.save(user);
                log.warn("admin user has been created without default password: admin, please change it");
            }
        };
    }
}
