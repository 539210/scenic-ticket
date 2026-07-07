USE scenic_ticket;

DROP PROCEDURE IF EXISTS sp_monthly_order_report;
DELIMITER //
CREATE PROCEDURE sp_monthly_order_report(IN p_year INT, IN p_month INT)
BEGIN
    SELECT
        DATE(created_at) AS order_date,
        COUNT(*) AS order_count,
        COALESCE(SUM(amount), 0) AS total_amount
    FROM orders
    WHERE YEAR(created_at) = p_year
      AND MONTH(created_at) = p_month
      AND status IN (1, 3)
    GROUP BY DATE(created_at)
    ORDER BY order_date;
END //
DELIMITER ;

DROP PROCEDURE IF EXISTS sp_update_inactive_items;
DELIMITER //
CREATE PROCEDURE sp_update_inactive_items(IN p_days_without_orders INT)
BEGIN
    UPDATE items i
    LEFT JOIN (
        SELECT item_id, MAX(created_at) AS latest_order_time
        FROM orders
        GROUP BY item_id
    ) o ON i.item_id = o.item_id
    SET i.status = 0,
        i.updated_at = CURRENT_TIMESTAMP
    WHERE i.status = 1
      AND (o.latest_order_time IS NULL OR o.latest_order_time < DATE_SUB(CURRENT_TIMESTAMP, INTERVAL p_days_without_orders DAY));
END //
DELIMITER ;
