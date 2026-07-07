USE scenic_ticket;

INSERT INTO users (user_id, username, password_hash, email, phone, role, status) VALUES
(1, 'admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin@example.com', '13800000001', 'ADMIN', 1),
(2, 'user001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user001@example.com', '13800000002', 'USER', 1),
(3, 'user002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user002@example.com', '13800000003', 'USER', 1),
(4, 'user003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user003@example.com', '13800000004', 'USER', 1),
(5, 'user004', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user004@example.com', '13800000005', 'USER', 1),
(6, 'user005', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user005@example.com', '13800000006', 'USER', 1),
(7, 'user006', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user006@example.com', '13800000007', 'USER', 1),
(8, 'user007', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user007@example.com', '13800000008', 'USER', 1),
(9, 'user008', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user008@example.com', '13800000009', 'USER', 1),
(10, 'user009', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user009@example.com', '13800000010', 'USER', 1)
ON DUPLICATE KEY UPDATE username = VALUES(username);

INSERT INTO categories (category_id, name, parent_id) VALUES
(1, '自然景观', NULL),
(2, '历史文化', NULL),
(3, '主题乐园', NULL),
(4, '山水风光', 1),
(5, '森林公园', 1),
(6, '古镇古街', 2),
(7, '博物展馆', 2),
(8, '亲子乐园', 3),
(9, '水上乐园', 3),
(10, '城市观光', NULL)
ON DUPLICATE KEY UPDATE name = VALUES(name), parent_id = VALUES(parent_id);

INSERT INTO items (item_id, title, category_id, status) VALUES
(1, '云岭山国家森林公园', 5, 1),
(2, '青河峡谷漂流', 4, 1),
(3, '明州古城墙', 6, 1),
(4, '海湾水世界', 9, 1),
(5, '星湖湿地公园', 1, 1),
(6, '城市观景塔', 10, 1),
(7, '民俗文化博物馆', 7, 1),
(8, '欢乐亲子乐园', 8, 1),
(9, '竹海栈道景区', 5, 1),
(10, '南山日出观景区', 4, 1)
ON DUPLICATE KEY UPDATE title = VALUES(title), category_id = VALUES(category_id), status = VALUES(status);

INSERT INTO orders (order_id, user_id, item_id, amount, status, created_at) VALUES
(1, 2, 1, 80.00, 1, '2026-07-01 09:10:00'),
(2, 3, 2, 120.00, 1, '2026-07-01 10:20:00'),
(3, 4, 3, 60.00, 3, '2026-07-02 11:30:00'),
(4, 5, 4, 150.00, 1, '2026-07-02 13:40:00'),
(5, 6, 5, 45.00, 0, '2026-07-03 08:15:00'),
(6, 7, 6, 98.00, 1, '2026-07-03 14:25:00'),
(7, 8, 7, 50.00, 2, '2026-07-04 15:35:00'),
(8, 9, 8, 110.00, 1, '2026-07-04 16:45:00'),
(9, 10, 9, 70.00, 3, '2026-07-05 09:55:00'),
(10, 2, 10, 88.00, 1, '2026-07-05 17:05:00')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status);

INSERT INTO profiles (profile_id, user_id, real_name, id_card, address, notes) VALUES
(1, 1, '管理员', '110101199001010001', '景区管理中心', '系统管理员账号'),
(2, 2, '张三', '110101199201010002', '北京市朝阳区示例路1号', '偏好自然景观'),
(3, 3, '李四', '110101199301010003', '上海市浦东新区示例路2号', '偏好水上项目'),
(4, 4, '王五', '110101199401010004', '广州市天河区示例路3号', '关注历史文化'),
(5, 5, '赵六', '110101199501010005', '深圳市南山区示例路4号', '亲子出游'),
(6, 6, '孙七', '110101199601010006', '杭州市西湖区示例路5号', '常购夜场票'),
(7, 7, '周八', '110101199701010007', '成都市武侯区示例路6号', '关注展馆活动'),
(8, 8, '吴九', '110101199801010008', '南京市秦淮区示例路7号', '周末游客'),
(9, 9, '郑十', '110101199901010009', '武汉市江汉区示例路8号', '偏好徒步景点'),
(10, 10, '钱一', '110101200001010010', '重庆市渝中区示例路9号', '摄影爱好者')
ON DUPLICATE KEY UPDATE real_name = VALUES(real_name), address = VALUES(address), notes = VALUES(notes);
