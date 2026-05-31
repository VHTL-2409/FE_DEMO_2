package com.example.demo.repository;

import com.example.demo.domain.entity.RoleName;
import com.example.demo.domain.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    @Query("SELECT COUNT(DISTINCT u.id) FROM User u JOIN u.roles r WHERE r.name = :role")
    long countUsersWithRole(@Param("role") RoleName role);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = :role")
    Page<User> findByRoleName(@Param("role") RoleName role, Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            LEFT JOIN StudentProfile sp ON sp.user.id = u.id
            WHERE r.name = :role
            AND (
              LOWER(u.username) LIKE :pat OR LOWER(u.email) LIKE :pat
              OR (sp IS NOT NULL AND (
                   LOWER(COALESCE(sp.fullName, '')) LIKE :pat
                OR LOWER(COALESCE(sp.displayName, '')) LIKE :pat
                OR LOWER(COALESCE(sp.phone, '')) LIKE :pat
              ))
            )
            """)
    Page<User> searchStudents(@Param("role") RoleName role, @Param("pat") String pattern, Pageable pageable);

    @Query("""
            SELECT DISTINCT u FROM User u
            JOIN u.roles r
            LEFT JOIN TeacherProfile tp ON tp.user.id = u.id
            WHERE r.name = :role
            AND (
              LOWER(u.username) LIKE :pat OR LOWER(u.email) LIKE :pat
              OR (tp IS NOT NULL AND (
                   LOWER(COALESCE(tp.fullName, '')) LIKE :pat
                OR LOWER(COALESCE(tp.displayName, '')) LIKE :pat
                OR LOWER(COALESCE(tp.phone, '')) LIKE :pat
              ))
            )
            """)
    Page<User> searchTeachers(@Param("role") RoleName role, @Param("pat") String pattern, Pageable pageable);
    Optional<User> findByUsername(String username);

    /** Một query: user + roles — dùng cho auth & CurrentUser */
    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles WHERE u.username = :username")
    Optional<User> findByUsernameWithRoles(@Param("username") String username);

    @Query("SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.roles")
    List<User> findAllWithRoles();

    Optional<User> findByEmail(String email);
    Optional<User> findByEmailIgnoreCase(String email);
    Optional<User> findByOauthUid(String oauthUid);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    long countByEmailVerifiedFalse();
}
