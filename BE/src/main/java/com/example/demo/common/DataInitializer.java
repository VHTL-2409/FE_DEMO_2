package com.example.demo.common;

import com.example.demo.domain.entity.*;
import com.example.demo.repository.ExamRepository;
import com.example.demo.repository.QuestionRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Set;

@Component
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final ExamRepository examRepository;
    private final QuestionRepository questionRepository;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    public DataInitializer(RoleRepository roleRepository,
                           UserRepository userRepository,
                           ExamRepository examRepository,
                           QuestionRepository questionRepository,
                           PasswordEncoder passwordEncoder,
                           JdbcTemplate jdbcTemplate) {
        this.roleRepository = roleRepository;
        this.userRepository = userRepository;
        this.examRepository = examRepository;
        this.questionRepository = questionRepository;
        this.passwordEncoder = passwordEncoder;
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) {
        ensureAttemptStatusConstraint();

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));
        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.TEACHER).build()));
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STUDENT).build()));

        userRepository.findByUsername("admin")
            .orElseGet(() -> userRepository.save(User.builder()
                .username("admin")
                .email("admin@demo.local")
                .password(passwordEncoder.encode("123456"))
                .roles(Set.of(adminRole))
                .build()));

        User teacher = userRepository.findByUsername("teacher1")
            .orElseGet(() -> userRepository.save(User.builder()
                .username("teacher1")
                .email("teacher1@demo.local")
                .password(passwordEncoder.encode("123456"))
                .roles(Set.of(teacherRole))
                .build()));

        userRepository.findByUsername("student1")
            .orElseGet(() -> userRepository.save(User.builder()
                .username("student1")
                .email("student1@demo.local")
                .password(passwordEncoder.encode("123456"))
                .roles(Set.of(studentRole))
                .build()));

        if (examRepository.count() == 0) {
            Exam exam = examRepository.save(Exam.builder()
                .title("Java Core Demo Exam")
                .description("Demo exam for online testing flow")
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now().plusDays(7))
                .durationMinutes(30)
                .isActive(true)
                .createdBy(teacher)
                .build());

            questionRepository.save(Question.builder()
                .exam(exam)
                .content("Java is ...?")
                .scoreWeight(1.0)
                .options("[{\"id\":\"A\",\"text\":\"Programming language\"},{\"id\":\"B\",\"text\":\"Database\"},{\"id\":\"C\",\"text\":\"OS\"},{\"id\":\"D\",\"text\":\"Browser\"}]")
                .correctAnswer("A")
                .build());

            questionRepository.save(Question.builder()
                .exam(exam)
                .content("Which keyword creates class instance?")
                .scoreWeight(1.0)
                .options("[{\"id\":\"A\",\"text\":\"this\"},{\"id\":\"B\",\"text\":\"new\"},{\"id\":\"C\",\"text\":\"class\"},{\"id\":\"D\",\"text\":\"extends\"}]")
                .correctAnswer("B")
                .build());
        }
    }

    private void ensureAttemptStatusConstraint() {
        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF EXISTS (
                    SELECT 1
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    WHERE t.relname = 'exam_attempts'
                      AND c.conname = 'exam_attempts_status_check'
                ) THEN
                    ALTER TABLE exam_attempts DROP CONSTRAINT exam_attempts_status_check;
                END IF;
            END $$;
            """);
        jdbcTemplate.execute("""
            ALTER TABLE exam_attempts
            ADD CONSTRAINT exam_attempts_status_check
            CHECK (status IN ('IN_PROGRESS', 'SUBMITTED', 'AUTO_SUBMITTED', 'STOPPED'))
            """);
    }
}
