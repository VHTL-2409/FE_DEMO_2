package com.example.demo.repository;

import com.example.demo.domain.entity.ClassEntity;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassRepository extends JpaRepository<ClassEntity, Long> {

    List<ClassEntity> findByTeacher(User teacher);

    List<ClassEntity> findByTeacherId(Long teacherId);

    List<ClassEntity> findByTeacherIdAndNameContainingIgnoreCase(Long teacherId, String keyword);

    Optional<ClassEntity> findByIdAndTeacherId(Long id, Long teacherId);

    Optional<ClassEntity> findByClassCode(String classCode);

    boolean existsByTeacherIdAndName(Long teacherId, String name);

    @Query("SELECT COUNT(cs) FROM ClassStudent cs WHERE cs.classEntity.id = :classId")
    long countStudentsByClassId(@Param("classId") Long classId);
}
