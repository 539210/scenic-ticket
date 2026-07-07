USE scenic_ticket;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'items'
      AND INDEX_NAME = 'idx_items_status_updated_at'
);
SET @sql := IF(
    @idx_exists = 0,
    'ALTER TABLE items ADD INDEX idx_items_status_updated_at (status, updated_at DESC, item_id DESC)',
    'SELECT ''idx_items_status_updated_at already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @idx_exists := (
    SELECT COUNT(1)
    FROM INFORMATION_SCHEMA.STATISTICS
    WHERE TABLE_SCHEMA = DATABASE()
      AND TABLE_NAME = 'orders'
      AND INDEX_NAME = 'idx_orders_created_status'
);
SET @sql := IF(
    @idx_exists = 0,
    'ALTER TABLE orders ADD INDEX idx_orders_created_status (created_at, status)',
    'SELECT ''idx_orders_created_status already exists'' AS message'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
