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

    Optional<ClassStudent> findByClassEntityAndStudent(ClassEntity classEntity, User student);

    Optional<ClassStudent> findByClassEntityIdAndStudentId(Long classId, Long studentId);

    boolean existsByClassEntityAndStudent(ClassEntity classEntity, User student);

    boolean existsByClassEntityIdAndStudentId(Long classId, Long studentId);

    void deleteByClassEntityAndStudent(ClassEntity classEntity, User student);

    void deleteByClassEntityIdAndStudentId(Long classId, Long studentId);

    long countByClassEntityId(Long classId);

    @Query("SELECT cs.student FROM ClassStudent cs WHERE cs.classEntity.teacher.id = :teacherId " +
           "AND cs.student.id NOT IN (SELECT cs2.student.id FROM ClassStudent cs2 WHERE cs2.classEntity.id = :classId) " +
           "AND cs.student.id IN (SELECT u.id FROM User u JOIN u.roles r WHERE r.name = 'STUDENT')")
    List<User> findAvailableStudents(@Param("teacherId") Long teacherId, @Param("classId") Long classId);

    @Query("SELECT cs.student FROM ClassStudent cs WHERE cs.classEntity.teacher.id = :teacherId " +
           "AND LOWER(cs.student.username) LIKE LOWER(CONCAT('%', :keyword, '%')) " +
           "AND cs.student.id NOT IN (SELECT cs2.student.id FROM ClassStudent cs2 WHERE cs2.classEntity.id = :classId) " +
           "AND cs.student.id IN (SELECT u.id FROM User u JOIN u.roles r WHERE r.name = 'STUDENT')")
    List<User> findAvailableStudentsByKeyword(@Param("teacherId") Long teacherId, @Param("classId") Long classId, @Param("keyword") String keyword);

    void deleteByClassEntity(ClassEntity classEntity);
}
