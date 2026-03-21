package com.example.demo.repository;

import com.example.demo.domain.entity.TeacherProfile;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TeacherProfileRepository extends JpaRepository<TeacherProfile, Long> {
    Optional<TeacherProfile> findByUser(User user);

    void deleteByUser(User user);

    @Query("SELECT DISTINCT tp FROM TeacherProfile tp JOIN FETCH tp.user u WHERE u.id IN :ids")
    List<TeacherProfile> findAllByUserIdInWithUser(@Param("ids") Collection<Long> ids);
}
