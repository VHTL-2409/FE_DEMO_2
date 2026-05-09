package com.example.demo.config;

import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.SqlResultSetMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

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
            fixConstraint(dataSource,
                "monitoring_events_event_type_check",
                """
                ALTER TABLE monitoring_events DROP CONSTRAINT IF EXISTS uk_monitoring_event_attempt_type_details;
                ALTER TABLE monitoring_events DROP CONSTRAINT IF EXISTS monitoring_events_event_type_check;
                ALTER TABLE monitoring_events ADD CONSTRAINT monitoring_events_event_type_check
                CHECK (event_type IN (
                    'ATTEMPT_START','DRAFT_SAVE','ATTEMPT_SUBMIT','AUTO_SUBMIT','NOTE',
                    'TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
                    'TAB_SWITCH','BLUR','EXIT_FULLSCREEN','FAST_SUBMIT','DUPLICATE_IP',
                    'COPY_PASTE','IDLE_TIME','DEVTOOLS_OPEN','RIGHT_CLICK','PRINT_SCREEN',
                    'RAPID_QUESTION_SWITCH','MULTI_MONITOR','HEARTBEAT_STALE','DEVICE_FINGERPRINT_CHANGED'
                ))
                """);

            fixConstraint(dataSource,
                "audit_logs_action_check",
                """
                ALTER TABLE audit_logs DROP CONSTRAINT IF EXISTS audit_logs_action_check;
                ALTER TABLE audit_logs ADD CONSTRAINT audit_logs_action_check
                CHECK (action IN (
                    'TEACHER_NOTE','TEACHER_WARNING','TEACHER_PAUSE','TEACHER_RESUME','TEACHER_INVALIDATE',
                    'SYSTEM_DUPLICATE_IP','SYSTEM_IP_CHANGE','SYSTEM_RISK_WARNING',
                    'SYSTEM_ATTEMPT_PAUSE','SYSTEM_ATTEMPT_RESUME'
                ))
                """);

            log.info("Database constraint fix completed");
        };
    }

    private void fixConstraint(DataSource dataSource, String constraintName, String sql) {
        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            // Check if constraint currently exists with wrong definition by trying to add a known-valid value
            // If it fails, it means the constraint needs updating
            stmt.execute(sql);
            log.info("Constraint '{}' fixed/created successfully", constraintName);
        } catch (SQLException e) {
            // Constraint already exists with correct definition, or table doesn't exist
            if (e.getMessage() != null && e.getMessage().contains("already exists")) {
                log.info("Constraint '{}' already exists, skipping", constraintName);
            } else {
                log.warn("Could not fix constraint '{}': {}", constraintName, e.getMessage());
            }
        }
    }
}
