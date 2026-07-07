USE scenic_ticket;

DROP TRIGGER IF EXISTS trg_orders_before_insert;
DELIMITER //
CREATE TRIGGER trg_orders_before_insert
BEFORE INSERT ON orders
FOR EACH ROW
BEGIN
    IF NEW.amount < 0 THEN
        SIGNAL SQLSTATE '45000'
            SET MESSAGE_TEXT = 'Order amount cannot be negative';
    END IF;

    IF NEW.status IS NULL THEN
        SET NEW.status = 0;
    END IF;
END //
DELIMITER ;

DROP TRIGGER IF EXISTS trg_items_before_update;
DELIMITER //
CREATE TRIGGER trg_items_before_update
BEFORE UPDATE ON items
FOR EACH ROW
BEGIN
    SET NEW.updated_at = CURRENT_TIMESTAMP;
END //
DELIMITER ;
