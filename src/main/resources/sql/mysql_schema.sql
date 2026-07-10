CREATE DATABASE IF NOT EXISTS scenic_ticket
    DEFAULT CHARACTER SET utf8mb4
    DEFAULT COLLATE utf8mb4_0900_ai_ci;

USE scenic_ticket;

CREATE TABLE IF NOT EXISTS schema_migrations (
    version VARCHAR(80) PRIMARY KEY,
    description VARCHAR(255) NOT NULL,
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS users (
    user_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    email VARCHAR(100) NOT NULL,
    phone VARCHAR(20),
    role ENUM('ADMIN', 'USER') NOT NULL DEFAULT 'USER',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=enabled, 0=disabled',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_users_username UNIQUE (username),
    CONSTRAINT uk_users_email UNIQUE (email),
    INDEX idx_users_role_status (role, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS categories (
    category_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL,
    parent_id BIGINT,
    CONSTRAINT fk_categories_parent
        FOREIGN KEY (parent_id) REFERENCES categories(category_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    INDEX idx_categories_parent_id (parent_id),
    INDEX idx_categories_name (name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS items (
    item_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(200) NOT NULL,
    category_id BIGINT NOT NULL,
    price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT 'ticket price controlled by admin',
    discount_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT 'discount percent, 0 means no discount',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1=available, 0=offline',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_items_category
        FOREIGN KEY (category_id) REFERENCES categories(category_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    INDEX idx_items_category_status (category_id, status),
    INDEX idx_items_title (title),
    INDEX idx_items_created_at (created_at),
    INDEX idx_items_status_updated_at (status, updated_at DESC, item_id DESC),
    CONSTRAINT chk_items_price CHECK (price >= 0),
    CONSTRAINT chk_items_discount_rate CHECK (discount_rate >= 0 AND discount_rate <= 100)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS orders (
    order_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    item_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00,
    discount_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00,
    payment_method VARCHAR(20) NOT NULL DEFAULT '微信',
    ticket_type_id BIGINT NULL COMMENT 'new ticket type reference; nullable for legacy orders',
    ticket_type_name_snapshot VARCHAR(50) NULL,
    original_unit_price DECIMAL(10, 2) NULL,
    discounted_unit_price DECIMAL(10, 2) NULL,
    visit_date DATE NULL,
    expires_at DATETIME NULL,
    paid_at DATETIME NULL,
    cancelled_at DATETIME NULL,
    completed_at DATETIME NULL,
    refunded_at DATETIME NULL,
    status_version INT NOT NULL DEFAULT 0,
    status TINYINT NOT NULL DEFAULT 0 COMMENT '0=pending, 1=paid, 2=cancelled, 3=completed',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_orders_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_orders_item
        FOREIGN KEY (item_id) REFERENCES items(item_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_orders_amount CHECK (amount >= 0),
    CONSTRAINT chk_orders_quantity CHECK (quantity > 0),
    CONSTRAINT chk_orders_unit_price CHECK (unit_price >= 0),
    CONSTRAINT chk_orders_discount_rate CHECK (discount_rate >= 0 AND discount_rate <= 100),
    CONSTRAINT chk_orders_status CHECK (status IN (0, 1, 2, 3)),
    CONSTRAINT chk_orders_original_unit_price CHECK (original_unit_price IS NULL OR original_unit_price >= 0),
    CONSTRAINT chk_orders_discounted_unit_price CHECK (discounted_unit_price IS NULL OR discounted_unit_price >= 0),
    INDEX idx_orders_user_created_at (user_id, created_at),
    INDEX idx_orders_item_status (item_id, status),
    INDEX idx_orders_status_created_at (status, created_at),
    INDEX idx_orders_created_status (created_at, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS profiles (
    profile_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    real_name VARCHAR(50),
    id_card VARCHAR(20),
    address VARCHAR(500),
    notes TEXT,
    CONSTRAINT uk_profiles_user_id UNIQUE (user_id),
    CONSTRAINT fk_profiles_user
        FOREIGN KEY (user_id) REFERENCES users(user_id)
        ON DELETE CASCADE
        ON UPDATE CASCADE,
    INDEX idx_profiles_real_name (real_name)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

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

CREATE TABLE IF NOT EXISTS refunds (
    refund_id BIGINT PRIMARY KEY AUTO_INCREMENT,
    order_id BIGINT NOT NULL,
    refund_amount DECIMAL(10, 2) NOT NULL,
    reason VARCHAR(500),
    status VARCHAR(20) NOT NULL DEFAULT 'SUCCESS',
    operator_user_id BIGINT NULL,
    refunded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_refunds_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_refunds_operator
        FOREIGN KEY (operator_user_id) REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
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
    CONSTRAINT fk_admissions_order
        FOREIGN KEY (order_id) REFERENCES orders(order_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_admissions_operator
        FOREIGN KEY (operator_user_id) REFERENCES users(user_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT chk_admissions_quantity CHECK (quantity > 0),
    INDEX idx_admissions_order_time (order_id, admitted_at),
    INDEX idx_admissions_operator_time (operator_user_id, admitted_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

INSERT INTO schema_migrations (version, description)
VALUES ('day09_full_schema', 'Full ticket type, inventory, order lifecycle, refund and admission schema')
ON DUPLICATE KEY UPDATE description = VALUES(description);
