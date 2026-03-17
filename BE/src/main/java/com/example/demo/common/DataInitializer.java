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
        ensureExamMonitoringColumns();
        ensureSingleRoleConstraint();
        cleanupUserRoles();

        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.ADMIN).build()));
        Role teacherRole = roleRepository.findByName(RoleName.TEACHER)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.TEACHER).build()));
        Role studentRole = roleRepository.findByName(RoleName.STUDENT)
            .orElseGet(() -> roleRepository.save(Role.builder().name(RoleName.STUDENT).build()));

        ensureRoleByUsernamePrefix(teacherRole, studentRole);
        cleanupProfilesByRole();

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

    private void ensureExamMonitoringColumns() {
        String[] cols = {
            "monitor_tab_switch", "monitor_blur", "monitor_exit_fullscreen", "monitor_copy_paste",
            "monitor_idle_time", "monitor_devtools", "monitor_duplicate_ip", "monitor_fast_submit",
            "monitor_right_click", "monitor_print_screen", "monitor_rapid_question_switch", "monitor_multi_monitor",
            "require_camera_mic"
        };
        for (String col : cols) {
            jdbcTemplate.execute("ALTER TABLE exams ADD COLUMN IF NOT EXISTS " + col + " BOOLEAN");
        }
        jdbcTemplate.execute("""
            UPDATE exams
            SET monitor_tab_switch = COALESCE(monitor_tab_switch, TRUE),
                monitor_blur = COALESCE(monitor_blur, TRUE),
                monitor_exit_fullscreen = COALESCE(monitor_exit_fullscreen, TRUE),
                monitor_copy_paste = COALESCE(monitor_copy_paste, TRUE),
                monitor_idle_time = COALESCE(monitor_idle_time, TRUE),
                monitor_devtools = COALESCE(monitor_devtools, TRUE),
                monitor_duplicate_ip = COALESCE(monitor_duplicate_ip, TRUE),
                monitor_fast_submit = COALESCE(monitor_fast_submit, TRUE),
                monitor_right_click = COALESCE(monitor_right_click, TRUE),
                monitor_print_screen = COALESCE(monitor_print_screen, TRUE),
                monitor_rapid_question_switch = COALESCE(monitor_rapid_question_switch, TRUE),
                monitor_multi_monitor = COALESCE(monitor_multi_monitor, TRUE),
                require_camera_mic = COALESCE(require_camera_mic, TRUE)
            """);
    }

    private void ensureSingleRoleConstraint() {
        jdbcTemplate.execute("""
            DO $$
            BEGIN
                IF NOT EXISTS (
                    SELECT 1
                    FROM pg_constraint c
                    JOIN pg_class t ON t.oid = c.conrelid
                    WHERE t.relname = 'user_roles'
                      AND c.conname = 'user_roles_user_id_unique'
                ) THEN
                    ALTER TABLE user_roles
                    ADD CONSTRAINT user_roles_user_id_unique UNIQUE (user_id);
                END IF;
            END $$;
            """);
    }

    private void cleanupUserRoles() {
        jdbcTemplate.execute("""
            DELETE FROM user_roles ur
            USING roles r
            WHERE ur.role_id = r.id
              AND r.name = 'STUDENT'
              AND ur.user_id IN (
                  SELECT user_id
                  FROM user_roles ur2
                  JOIN roles r2 ON ur2.role_id = r2.id
                  WHERE r2.name = 'TEACHER'
              );
            """);
    }

    private void ensureRoleByUsernamePrefix(Role teacherRole, Role studentRole) {
        jdbcTemplate.update("""
            INSERT INTO user_roles (user_id, role_id)
            SELECT u.id, ?
            FROM users u
            WHERE LOWER(u.username) LIKE 'teacher%'
              AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id)
            """, teacherRole.getId());

        jdbcTemplate.update("""
            INSERT INTO user_roles (user_id, role_id)
            SELECT u.id, ?
            FROM users u
            WHERE LOWER(u.username) LIKE 'student%'
              AND NOT EXISTS (SELECT 1 FROM user_roles ur WHERE ur.user_id = u.id)
            """, studentRole.getId());
    }

    private void cleanupProfilesByRole() {
        // Xóa teacher_profiles cho user có role STUDENT (giữ student_profile)
        jdbcTemplate.execute("""
            DELETE FROM teacher_profiles tp
            USING user_roles ur
            JOIN roles r ON ur.role_id = r.id
            WHERE tp.user_id = ur.user_id
              AND r.name = 'STUDENT';
            """);

        // Xóa student_profiles cho user có role TEACHER (giữ teacher_profile)
        jdbcTemplate.execute("""
            DELETE FROM student_profiles sp
            USING user_roles ur
            JOIN roles r ON ur.role_id = r.id
            WHERE sp.user_id = ur.user_id
              AND r.name = 'TEACHER';
            """);

        // User có cả 2 profile: giữ profile phù hợp username, xóa profile còn lại
        // username teacher% hoặc admin% -> giữ teacher_profile, xóa student_profile
        jdbcTemplate.execute("""
            DELETE FROM student_profiles sp
            WHERE sp.user_id IN (
                SELECT sp2.user_id
                FROM student_profiles sp2
                JOIN teacher_profiles tp ON tp.user_id = sp2.user_id
                JOIN users u ON u.id = sp2.user_id
                WHERE LOWER(u.username) LIKE 'teacher%' OR LOWER(u.username) LIKE 'admin%'
            );
            """);

        // username student% hoặc khác -> giữ student_profile, xóa teacher_profile
        jdbcTemplate.execute("""
            DELETE FROM teacher_profiles tp
            WHERE tp.user_id IN (
                SELECT tp2.user_id
                FROM teacher_profiles tp2
                JOIN student_profiles sp ON sp.user_id = tp2.user_id
                JOIN users u ON u.id = tp2.user_id
                WHERE LOWER(u.username) LIKE 'student%'
                   OR (LOWER(u.username) NOT LIKE 'teacher%' AND LOWER(u.username) NOT LIKE 'admin%')
            );
            """);
    }
}
