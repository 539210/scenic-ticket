USE scenic_ticket;

CREATE OR REPLACE VIEW v_user_profile AS
SELECT
    u.user_id,
    u.username,
    u.email,
    u.phone,
    u.role,
    u.status,
    p.real_name,
    p.id_card,
    p.address,
    u.created_at,
    u.updated_at
FROM users u
LEFT JOIN profiles p ON u.user_id = p.user_id;

CREATE OR REPLACE VIEW v_item_order_summary AS
SELECT
    i.item_id,
    i.title,
    c.name AS category_name,
    i.status AS item_status,
    COUNT(o.order_id) AS order_count,
    COALESCE(SUM(CASE WHEN o.status IN (1, 3) THEN o.amount ELSE 0 END), 0) AS paid_amount,
    MAX(o.created_at) AS latest_order_time
FROM items i
JOIN categories c ON i.category_id = c.category_id
LEFT JOIN orders o ON i.item_id = o.item_id
GROUP BY i.item_id, i.title, c.name, i.status;
