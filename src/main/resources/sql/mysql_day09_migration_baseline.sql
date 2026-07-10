USE scenic_ticket;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO schema_migrations (version, description)
VALUES ('day09_migration_baseline', 'Introduce versioned forward migrations without removing course fields')
ON DUPLICATE KEY UPDATE description = VALUES(description);
