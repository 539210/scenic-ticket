USE scenic_ticket;

INSERT INTO users (user_id, username, password_hash, email, phone, role, status) VALUES
(1, 'kongsc', '$2a$10$MR/69uuomrtndPeQl3LZ.uXNLHRS2QkThMDTLfLjf7LAarLY4HtOi', 'kongsc@example.com', '13800000001', 'ADMIN', 1),
(2, 'user001', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user001@example.com', '13800000002', 'USER', 1),
(3, 'user002', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user002@example.com', '13800000003', 'USER', 1),
(4, 'user003', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user003@example.com', '13800000004', 'USER', 1),
(5, 'user004', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user004@example.com', '13800000005', 'USER', 1),
(6, 'user005', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user005@example.com', '13800000006', 'USER', 1),
(7, 'user006', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user006@example.com', '13800000007', 'USER', 1),
(8, 'user007', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user007@example.com', '13800000008', 'USER', 1),
(9, 'user008', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user008@example.com', '13800000009', 'USER', 1),
(10, 'user009', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'user009@example.com', '13800000010', 'USER', 1)
ON DUPLICATE KEY UPDATE
    username = VALUES(username),
    password_hash = VALUES(password_hash),
    email = VALUES(email),
    phone = VALUES(phone),
    role = VALUES(role),
    status = VALUES(status);

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
(10, '南山日出观景区', 4, 1),
(11, '云海索道观景区', 4, 1),
(12, '花溪湿地科普园', 1, 1),
(13, '北岸古街夜游区', 6, 1),
(14, '城市艺术展览馆', 7, 1),
(15, '星河亲子营地', 8, 1),
(16, '蓝湾水上运动中心', 9, 1),
(17, '红枫森林步道', 5, 1),
(18, '东湖游船码头', 1, 1),
(19, '老城钟楼文化区', 2, 1),
(20, '云顶露营观星台', 4, 1)
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
(10, 2, 10, 88.00, 1, '2026-07-05 17:05:00'),
(11, 3, 11, 135.00, 1, '2026-07-06 09:20:00'),
(12, 4, 12, 58.00, 0, '2026-07-06 10:35:00'),
(13, 5, 13, 90.00, 1, '2026-07-06 14:05:00'),
(14, 6, 14, 40.00, 3, '2026-07-07 11:15:00'),
(15, 7, 15, 128.00, 1, '2026-07-07 15:50:00'),
(16, 8, 16, 168.00, 1, '2026-07-08 09:45:00'),
(17, 9, 17, 75.00, 2, '2026-07-08 12:30:00'),
(18, 10, 18, 96.00, 1, '2026-07-08 16:20:00'),
(19, 2, 19, 52.00, 3, '2026-07-09 10:10:00'),
(20, 3, 20, 188.00, 0, '2026-07-09 18:40:00')
ON DUPLICATE KEY UPDATE amount = VALUES(amount), status = VALUES(status);

INSERT INTO profiles (profile_id, user_id, real_name, id_card, address, notes) VALUES
(1, 1, '孔思成', '110101199001010001', '景区管理中心', '系统管理员账号'),
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
