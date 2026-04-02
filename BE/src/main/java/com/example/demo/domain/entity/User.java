package com.example.demo.domain.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "users")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 100)
    private String username;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(name = "email_verified")
    @Builder.Default
    private Boolean emailVerified = true;

    @Column(length = 100)
    private String fullName;

    @Column(length = 50)
    private String studentCode;

    @Column(length = 20)
    private String phone;

    @Column(length = 200)
    private String address;

    @Column(length = 50)
    private String grade;

    @Column(length = 100)
    private String faculty;

    @Column(name = "enabled")
    @Builder.Default
    private Boolean enabled = true;

    /** LAZY + JOIN FETCH ở repository khi cần roles — tránh load roles mọi lúc */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "user_roles",
        joinColumns = @JoinColumn(name = "user_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    @Builder.Default
    private Set<Role> roles = new HashSet<>();
}
