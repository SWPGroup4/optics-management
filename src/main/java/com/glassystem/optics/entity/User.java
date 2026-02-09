package com.glassystem.optics.entity;


import java.time.LocalDate;
import java.util.List;
import java.util.Set;

import com.glassystem.optics.enums.UserStatus;
import com.glassystem.optics.validatory.DobConstraint;
import com.glassystem.optics.validatory.Gmail;
import com.glassystem.optics.validatory.VietNamPhone;
import jakarta.persistence.*;

import jakarta.validation.constraints.Size;
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

    @Size(min = 3, message = "USERNAME_INVALID")
    String username;
    @Size(min = 8, message = "PASSWORD_INVALID")
    String password;
    String firstName;
    String lastName;
    @DobConstraint(min = 10, message = "INVALID_DOB")
    LocalDate dob;

    String imageUrl;
    @Gmail(message = "INVALID_GMAIL")
    String email;
    @VietNamPhone(message = "INVALID_VNPHONE")
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

    @OneToMany(mappedBy = "customer", cascade = CascadeType.ALL)
     List<Orders> orders;
}
