package com.glassystem.optics.entity;


import java.time.LocalDate;
import java.util.Set;

import com.glassystem.optics.enums.UserStatus;
import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "users")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;
    String username;
    String password;
    String firstName;
    String lastName;
    LocalDate dob;

    String imageUrl;
    String email;
    String phone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    UserStatus status;

    @ManyToMany(fetch = FetchType.LAZY)
            @JoinTable(
                            name = "users_roles",
                            joinColumns = @JoinColumn(name = "user_id"),
                            inverseJoinColumns = @JoinColumn(name = "role_name")
            )
    Set<Role> roles;
}
