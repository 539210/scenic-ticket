USE scenic_ticket;

DROP PROCEDURE IF EXISTS sp_monthly_order_report;
DELIMITER //
CREATE PROCEDURE sp_monthly_order_report(IN p_year INT, IN p_month INT)
BEGIN
    DECLARE v_start_date DATE;
    DECLARE v_end_date DATE;

    SET v_start_date = STR_TO_DATE(CONCAT(p_year, '-', LPAD(p_month, 2, '0'), '-01'), '%Y-%m-%d');
    SET v_end_date = DATE_ADD(v_start_date, INTERVAL 1 MONTH);

    SELECT
        DATE(created_at) AS order_date,
        COUNT(*) AS order_count,
        COALESCE(SUM(amount), 0) AS total_amount
    FROM orders
    WHERE created_at >= v_start_date
      AND created_at < v_end_date
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
