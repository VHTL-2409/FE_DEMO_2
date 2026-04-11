package com.example.demo.repository;

import com.example.demo.domain.entity.ClassEntity;
import com.example.demo.domain.entity.ClassStudent;
import com.example.demo.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ClassStudentRepository extends JpaRepository<ClassStudent, Long> {

    List<ClassStudent> findByClassEntity(ClassEntity classEntity);

    List<ClassStudent> findByClassEntityId(Long classId);

    List<ClassStudent> findByStudent(User student);

    List<ClassStudent> findByStudentId(Long studentId);

    @Query("""
           SELECT cs
           FROM ClassStudent cs
           JOIN FETCH cs.classEntity c
           JOIN FETCH c.teacher
           WHERE cs.student.id = :studentId
           """)
    List<ClassStudent> findByStudentIdWithClassAndTeacher(@Param("studentId") Long studentId);

    Optional<ClassStudent> findByClassEntityAndStudent(ClassEntity classEntity, User student);

    Optional<ClassStudent> findByClassEntityIdAndStudentId(Long classId, Long studentId);

    boolean existsByClassEntityAndStudent(ClassEntity classEntity, User student);

    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);

    void deleteByClassEntityAndStudent(ClassEntity classEntity, User student);

    void deleteByClassEntityIdAndStudentId(Long classId, Long studentId);

    long countByClassEntityId(Long classId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'STUDENT' " +
           "AND u.id NOT IN (SELECT cs.student.id FROM ClassStudent cs WHERE cs.classEntity.id = :classId)")
    List<User> findAvailableStudents(@Param("classId") Long classId);

    @Query("SELECT u FROM User u JOIN u.roles r WHERE r.name = 'STUDENT' " +
           "AND (LOWER(u.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "OR LOWER(COALESCE(u.email, '')) LIKE LOWER(CONCAT('%', :keyword, '%'))) " +
           "AND u.id NOT IN (SELECT cs.student.id FROM ClassStudent cs WHERE cs.classEntity.id = :classId)")
    List<User> findAvailableStudentsByKeyword(@Param("classId") Long classId, @Param("keyword") String keyword);

    void deleteByClassEntity(ClassEntity classEntity);
}
