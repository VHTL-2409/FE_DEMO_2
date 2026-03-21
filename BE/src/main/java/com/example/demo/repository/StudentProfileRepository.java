package com.example.demo.repository;

import com.example.demo.domain.entity.StudentProfile;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {
    Optional<StudentProfile> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT DISTINCT sp FROM StudentProfile sp JOIN FETCH sp.user u WHERE u.id IN :ids")
    List<StudentProfile> findAllByUserIdInWithUser(@Param("ids") Collection<Long> ids);
}
