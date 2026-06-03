package com.example.demo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

@Configuration
public class DatabaseConstraintFixConfig {

    private static final Logger log = LoggerFactory.getLogger(DatabaseConstraintFixConfig.class);

    @Bean
    public ApplicationRunner fixDatabaseConstraints(DataSource dataSource) {
        return args -> {
            executeCompatibilitySql(dataSource,
                "waiting attempt schema",
                """
                ALTER TABLE IF EXISTS exam_attempts ADD COLUMN IF NOT EXISTS joined_at TIMESTAMP;
                ALTER TABLE IF EXISTS exam_attempts ALTER COLUMN started_at DROP NOT NULL;
                UPDATE exam_attempts
                SET joined_at = started_at
                WHERE joined_at IS NULL
                  AND started_at IS NOT NULL
                """);

            executeCompatibilitySql(dataSource,
                "student identity profile schema",
                """
                ALTER TABLE IF EXISTS student_profiles ADD COLUMN IF NOT EXISTS citizen_id VARCHAR(20)
                """);

            executeCompatibilitySql(dataSource,
                "exam_attempts_status_check",
                """
                ALTER TABLE IF EXISTS exam_attempts DROP CONSTRAINT IF EXISTS exam_attempts_status_check;
                ALTER TABLE IF EXISTS exam_attempts ADD CONSTRAINT exam_attempts_status_check
                CHECK (status IN ('WAITING', 'IN_PROGRESS', 'PAUSED', 'SUBMITTED', 'AUTO_SUBMITTED', 'STOPPED'))
                """);

            executeCompatibilitySql(dataSource,
                "monitoring_events_event_type_check",
                """
                ALTER TABLE IF EXISTS monitoring_events DROP CONSTRAINT IF EXISTS uk_monitoring_event_attempt_type_details;
                ALTER TABLE IF EXISTS monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
                ALTER TABLE IF EXISTS monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
                CHECK (event_type IN (
                    'ATTEMPT_START','DRAFT_SAVE','ATTEMPT_SUBMIT','AUTO_SUBMIT','NOTE',
                    'TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
                    'TAB_SWITCH','BLUR','EXIT_FULLSCREEN','FAST_SUBMIT','DUPLICATE_IP',
                    'COPY_PASTE','IDLE_TIME','DEVTOOLS_OPEN','RIGHT_CLICK','PRINT_SCREEN',
                    'RAPID_QUESTION_SWITCH','MULTI_MONITOR','HEARTBEAT_STALE','DEVICE_FINGERPRINT_CHANGED',
                    'RULES_AGREEMENT','IDENTITY_REVIEW_REQUIRED','IDENTITY_RECHECK'
                ))
                """);

            executeCompatibilitySql(dataSource,
                "audit_logs_action_check",
                """
                ALTER TABLE IF EXISTS audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
                ALTER TABLE IF EXISTS audit_logs ADD CONSTRAINT audit_logs_action_check
                CHECK (action IN (
                    'TEACHER_NOTE','TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
                    'SYSTEM_DUPLICATE_IP','SYSTEM_IP_CHANGE','SYSTEM_RISK_WARNING',
                    'SYSTEM_ATTEMPT_PAUSE','SYSTEM_ATTEMPT_RESUME'
                ))
                """);

            log.info("Database constraint fix completed");
        };
    }

    private void executeCompatibilitySql(DataSource dataSource, String label, String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute(sql);
            log.info("Database compatibility step '{}' completed", label);
        } catch (SQLException e) {
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("Database compatibility step '{}' already applied", label);
            } else {
                log.warn("Could not run database compatibility step '{}': {}", label, e.getMessage());
            }
        }
    }
}
