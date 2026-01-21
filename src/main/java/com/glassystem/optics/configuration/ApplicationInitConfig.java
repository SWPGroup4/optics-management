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
public class ApplicationInitConfig {

    private final UserRepository userRepository;

    @Bean
    ApplicationRunner applicationRunner() {
        return args -> {
            try {
                if (userRepository.count() == 0) {
                    User admin = new User();
                    admin.setUsername("admin");
                    admin.setPassword("123456");
                    userRepository.save(admin);
                }
            } catch (Exception e) {
                // BỎ QUA nếu bảng chưa sẵn sàng
            }
        };
    }
}
