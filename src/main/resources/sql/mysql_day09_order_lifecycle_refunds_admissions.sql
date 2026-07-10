USE scenic_ticket;

DROP PROCEDURE IF EXISTS add_column_if_missing;

DELIMITER //

CREATE PROCEDURE add_column_if_missing(
    IN table_name_value VARCHAR(64),
    IN column_name_value VARCHAR(64),
    IN ddl_value TEXT
)
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = DATABASE()
          AND table_name = table_name_value
          AND column_name = column_name_value
    ) THEN
        SET @ddl = ddl_value;
        PREPARE stmt FROM @ddl;
        EXECUTE stmt;
        DEALLOCATE PREPARE stmt;
    END IF;
END//

DELIMITER ;

CALL add_column_if_missing('orders', 'ticket_type_id',
    'ALTER TABLE orders ADD COLUMN ticket_type_id BIGINT NULL COMMENT ''new ticket type reference; nullable for legacy orders'' AFTER item_id');
CALL add_column_if_missing('orders', 'ticket_type_name_snapshot',
    'ALTER TABLE orders ADD COLUMN ticket_type_name_snapshot VARCHAR(50) NULL AFTER ticket_type_id');
CALL add_column_if_missing('orders', 'original_unit_price',
    'ALTER TABLE orders ADD COLUMN original_unit_price DECIMAL(10,2) NULL AFTER ticket_type_name_snapshot');
CALL add_column_if_missing('orders', 'discounted_unit_price',
    'ALTER TABLE orders ADD COLUMN discounted_unit_price DECIMAL(10,2) NULL AFTER discount_rate');
CALL add_column_if_missing('orders', 'visit_date',
    'ALTER TABLE orders ADD COLUMN visit_date DATE NULL AFTER payment_method');
CALL add_column_if_missing('orders', 'expires_at',
    'ALTER TABLE orders ADD COLUMN expires_at DATETIME NULL AFTER created_at');
CALL add_column_if_missing('orders', 'paid_at',
    'ALTER TABLE orders ADD COLUMN paid_at DATETIME NULL AFTER expires_at');
CALL add_column_if_missing('orders', 'cancelled_at',
    'ALTER TABLE orders ADD COLUMN cancelled_at DATETIME NULL AFTER paid_at');
CALL add_column_if_missing('orders', 'completed_at',
    'ALTER TABLE orders ADD COLUMN completed_at DATETIME NULL AFTER cancelled_at');
CALL add_column_if_missing('orders', 'refunded_at',
    'ALTER TABLE orders ADD COLUMN refunded_at DATETIME NULL AFTER completed_at');
CALL add_column_if_missing('orders', 'status_version',
    'ALTER TABLE orders ADD COLUMN status_version INT NOT NULL DEFAULT 0 AFTER refunded_at');

DROP PROCEDURE add_column_if_missing;

UPDATE orders o
JOIN ticket_types tt ON tt.item_id = o.item_id AND tt.name = '成人票'
SET o.ticket_type_id = COALESCE(o.ticket_type_id, tt.ticket_type_id),
    o.ticket_type_name_snapshot = COALESCE(o.ticket_type_name_snapshot, tt.name),
    o.original_unit_price = COALESCE(o.original_unit_price, o.unit_price),
    o.discounted_unit_price = COALESCE(
        o.discounted_unit_price,
        ROUND(o.unit_price * (1 - o.discount_rate / 100), 2)
    ),
    o.visit_date = COALESCE(o.visit_date, DATE(o.created_at)),
    o.expires_at = COALESCE(o.expires_at, DATE_ADD(o.created_at, INTERVAL 15 MINUTE)),
    o.paid_at = CASE WHEN o.status IN (1, 3) THEN COALESCE(o.paid_at, o.created_at) ELSE o.paid_at END,
    o.cancelled_at = CASE WHEN o.status = 2 THEN COALESCE(o.cancelled_at, o.created_at) ELSE o.cancelled_at END,
    o.completed_at = CASE WHEN o.status = 3 THEN COALESCE(o.completed_at, o.created_at) ELSE o.completed_at END;

SET @fk_orders_ticket_type_exists := (
    SELECT COUNT(1)
    FROM information_schema.referential_constraints
    WHERE constraint_schema = DATABASE()
      AND constraint_name = 'fk_orders_ticket_type'
);
SET @fk_orders_ticket_type_sql := IF(
    @fk_orders_ticket_type_exists = 0,
    'ALTER TABLE orders ADD CONSTRAINT fk_orders_ticket_type FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(ticket_type_id) ON DELETE RESTRICT ON UPDATE CASCADE',
    'SELECT ''fk_orders_ticket_type already exists'' AS message'
);
PREPARE stmt FROM @fk_orders_ticket_type_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS refunds (
    refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    operator_user_id BIGINT NULL,
    refunded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_refunds_operator FOREIGN KEY (operator_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL ON UPDATE CASCADE,
    CONSTRAINT uk_refunds_order UNIQUE (order_id),
    CONSTRAINT chk_refunds_amount CHECK (refund_amount >= 0),
    INDEX idx_refunds_refunded_at (refunded_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS admissions (
    admission_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    quantity INT NOT NULL,
    operator_user_id BIGINT NOT NULL,
    admitted_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    note VARCHAR(500),
    CONSTRAINT fk_admissions_order FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT fk_admissions_operator FOREIGN KEY (operator_user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT ON UPDATE CASCADE,
    CONSTRAINT chk_admissions_quantity CHECK (quantity > 0),
    INDEX idx_admissions_order_time (order_id, admitted_at),
    INDEX idx_admissions_operator_time (operator_user_id, admitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO schema_migrations (version, description)
VALUES ('day09_order_lifecycle_refunds_admissions', 'Extend legacy orders and add refund/admission records')
ON DUPLICATE KEY UPDATE description = VALUES(description);
