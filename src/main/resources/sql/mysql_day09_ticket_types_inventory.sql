USE scenic_ticket;

CREATE TABLE IF NOT EXISTS ticket_types (
    ticket_type_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    item_id BIGINT NOT NULL,
    name VARCHAR(50) NOT NULL,
    original_price DECIMAL(10, 2) NOT NULL,
    discount_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=available, 0=offline',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_types_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uk_ticket_types_item_name UNIQUE (item_id, name),
    CONSTRAINT chk_ticket_types_price CHECK (original_price >= 0),
    CONSTRAINT chk_ticket_types_discount CHECK (discount_rate >= 0 AND discount_rate <= 100),
    CONSTRAINT chk_ticket_types_status CHECK (status IN (0, 1)),
    INDEX idx_ticket_types_item_status (item_id, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS ticket_inventory (
    inventory_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    ticket_type_id BIGINT NOT NULL,
    visit_date DATE NOT NULL,
    total_stock INT NOT NULL,
    available_stock INT NOT NULL,
    reserved_stock INT NOT NULL DEFAULT 0,
    sold_stock INT NOT NULL DEFAULT 0,
    version INT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_ticket_inventory_type
        FOREIGN KEY (ticket_type_id) REFERENCES ticket_types(ticket_type_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT uk_ticket_inventory_type_date UNIQUE (ticket_type_id, visit_date),
    CONSTRAINT chk_ticket_inventory_total CHECK (total_stock >= 0),
    CONSTRAINT chk_ticket_inventory_available CHECK (available_stock >= 0),
    CONSTRAINT chk_ticket_inventory_reserved CHECK (reserved_stock >= 0),
    CONSTRAINT chk_ticket_inventory_sold CHECK (sold_stock >= 0),
    CONSTRAINT chk_ticket_inventory_balance CHECK (
        available_stock + reserved_stock + sold_stock <= total_stock
    ),
    INDEX idx_ticket_inventory_date_available (visit_date, available_stock),
    INDEX idx_ticket_inventory_type_date (ticket_type_id, visit_date)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO ticket_types (item_id, name, original_price, discount_rate, status)
SELECT item_id, '成人票', price, discount_rate, status
FROM items
ON DUPLICATE KEY UPDATE
    original_price = VALUES(original_price),
    discount_rate = VALUES(discount_rate),
    status = VALUES(status);

INSERT INTO ticket_types (item_id, name, original_price, discount_rate, status)
SELECT item_id, '儿童票', ROUND(price * 0.50, 2), discount_rate, status
FROM items
ON DUPLICATE KEY UPDATE
    original_price = VALUES(original_price),
    discount_rate = VALUES(discount_rate),
    status = VALUES(status);

INSERT INTO ticket_types (item_id, name, original_price, discount_rate, status)
SELECT item_id, '学生票', ROUND(price * 0.80, 2), discount_rate, status
FROM items
ON DUPLICATE KEY UPDATE
    original_price = VALUES(original_price),
    discount_rate = VALUES(discount_rate),
    status = VALUES(status);

INSERT INTO ticket_inventory (
    ticket_type_id, visit_date, total_stock, available_stock, reserved_stock, sold_stock
)
SELECT tt.ticket_type_id, DATE_ADD(CURRENT_DATE, INTERVAL offsets.day_offset DAY), 100, 100, 0, 0
FROM ticket_types tt
JOIN (
    SELECT 1 AS day_offset UNION ALL SELECT 2 UNION ALL SELECT 3 UNION ALL SELECT 4
    UNION ALL SELECT 5 UNION ALL SELECT 6 UNION ALL SELECT 7
) offsets
ON 1 = 1
ON DUPLICATE KEY UPDATE
    total_stock = GREATEST(total_stock, reserved_stock + sold_stock),
    available_stock = GREATEST(total_stock, reserved_stock + sold_stock) - reserved_stock - sold_stock;

INSERT INTO schema_migrations (version, description)
VALUES ('day09_ticket_types_inventory', 'Add ticket types and per-date inventory with non-negative balance constraints')
ON DUPLICATE KEY UPDATE description = VALUES(description);
