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

CALL add_column_if_missing('items', 'price',
    'ALTER TABLE items ADD COLUMN price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 COMMENT ''ticket price controlled by admin'' AFTER category_id');
CALL add_column_if_missing('items', 'discount_rate',
    'ALTER TABLE items ADD COLUMN discount_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00 COMMENT ''discount percent, 0 means no discount'' AFTER price');
CALL add_column_if_missing('orders', 'quantity',
    'ALTER TABLE orders ADD COLUMN quantity INT NOT NULL DEFAULT 1 AFTER amount');
CALL add_column_if_missing('orders', 'unit_price',
    'ALTER TABLE orders ADD COLUMN unit_price DECIMAL(10, 2) NOT NULL DEFAULT 0.00 AFTER quantity');
CALL add_column_if_missing('orders', 'discount_rate',
    'ALTER TABLE orders ADD COLUMN discount_rate DECIMAL(5, 2) NOT NULL DEFAULT 0.00 AFTER unit_price');
CALL add_column_if_missing('orders', 'payment_method',
    'ALTER TABLE orders ADD COLUMN payment_method VARCHAR(20) NOT NULL DEFAULT ''微信'' AFTER discount_rate');

DROP PROCEDURE add_column_if_missing;

UPDATE items
SET price = CASE item_id
    WHEN 1 THEN 80.00
    WHEN 2 THEN 120.00
    WHEN 3 THEN 60.00
    WHEN 4 THEN 150.00
    WHEN 5 THEN 45.00
    WHEN 6 THEN 98.00
    WHEN 7 THEN 50.00
    WHEN 8 THEN 110.00
    WHEN 9 THEN 70.00
    WHEN 10 THEN 88.00
    WHEN 11 THEN 135.00
    WHEN 12 THEN 58.00
    WHEN 13 THEN 90.00
    WHEN 14 THEN 40.00
    WHEN 15 THEN 128.00
    WHEN 16 THEN 168.00
    WHEN 17 THEN 75.00
    WHEN 18 THEN 96.00
    WHEN 19 THEN 52.00
    WHEN 20 THEN 188.00
    ELSE price
END,
discount_rate = 0.00
WHERE item_id BETWEEN 1 AND 20;

UPDATE orders o
JOIN items i ON i.item_id = o.item_id
SET o.quantity = CASE WHEN o.quantity <= 0 THEN 1 ELSE o.quantity END,
    o.unit_price = CASE WHEN o.unit_price = 0.00 THEN i.price ELSE o.unit_price END,
    o.discount_rate = CASE WHEN o.discount_rate IS NULL THEN 0.00 ELSE o.discount_rate END,
    o.payment_method = CASE
        WHEN o.payment_method IS NULL OR o.payment_method = '' THEN '微信'
        ELSE o.payment_method
    END;
