package com.glassystem.optics.entity;


import java.time.LocalDate;
import java.util.Set;

import jakarta.persistence.*;

import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

@Entity
@Table(
        name = "users",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "username")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    String id;

    //xu ly ma register user cung luc
    @Column(
            name = "username",
            nullable = false,
            columnDefinition = "citext"
    )

    String username;
    String password;
    String firstName;
    String lastName;
    LocalDate dob;

    @ManyToMany
    Set<Role> roles;
}
