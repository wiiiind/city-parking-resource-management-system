USE city_parking_resource_management;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '管理员', '负责系统配置、资源管理和全局监控'),
(2, 'LOT_MANAGER', '停车场管理员', '负责停车场资源维护与订单处理'),
(3, 'OWNER', '车主', '负责车辆维护、停车查询和订单查看');

INSERT INTO sys_user (id, username, password, real_name, phone, role_name, status) VALUES
(1, 'admin', '123456', '系统管理员', '13800000001', '管理员', '启用'),
(2, 'manager', '123456', '停车场主管', '13800000002', '停车场管理员', '启用'),
(3, 'owner', '123456', '张晓彤', '13800000003', '车主', '启用'),
(4, 'owner2', '123456', '李思雨', '13800000004', '车主', '启用');

INSERT INTO sys_user_role (id, user_id, role_id) VALUES
(1, 1, 1),
(2, 2, 2),
(3, 3, 3),
(4, 4, 3);

INSERT INTO biz_vehicle (id, user_id, plate_number, brand, color, status) VALUES
(1, 3, '沪A12345', '比亚迪海豹', '白色', '正常'),
(2, 3, '沪B66218', '特斯拉Model 3', '灰色', '正常'),
(3, 2, '沪C90871', '大众帕萨特', '黑色', '正常'),
(4, 1, '沪D33688', '丰田凯美瑞', '银色', '正常'),
(5, 4, '浙A77881', '理想L6', '蓝色', '正常');

INSERT INTO biz_parking_lot (id, name, code, address, manager_name, business_hours, total_spaces, occupied_spaces, free_spaces, status) VALUES
(1, '人民广场地下停车场', 'PD-001', '上海市黄浦区人民大道 88 号', '徐晨', '06:00-23:30', 24, 0, 24, '运营中'),
(2, '滨江商务停车中心', 'PD-002', '上海市浦东新区滨江大道 166 号', '高宇', '全天开放', 20, 0, 20, '运营中'),
(3, '静安嘉里地下停车库', 'PD-003', '上海市静安区南京西路 1515 号', '林杰', '06:30-23:00', 28, 0, 28, '运营中'),
(4, '陆家嘴金融城停车场', 'PD-004', '上海市浦东新区世纪大道 100 号', '宋宁', '全天开放', 36, 0, 36, '运营中'),
(5, '徐家汇商圈停车楼', 'PD-005', '上海市徐汇区虹桥路 1 号', '何川', '07:00-23:30', 26, 0, 26, '运营中'),
(6, '杭州钱江新城停车中心', 'PD-006', '杭州市上城区解放东路 88 号', '赵航', '全天开放', 32, 0, 32, '运营中'),
(7, '苏州金鸡湖休闲停车场', 'PD-007', '苏州市工业园区观枫街 399 号', '沈悦', '08:00-22:30', 18, 0, 18, '运营中'),
(8, '南京新街口观光停车库', 'PD-008', '南京市秦淮区汉中路 18 号', '顾明', '06:00-22:00', 22, 0, 22, '运营中'),
(9, '宁波东部新城地下停车场', 'PD-009', '宁波市鄞州区海晏北路 36 号', '潘越', '全天开放', 30, 0, 30, '运营中'),
(10, '无锡太湖广场综合停车场', 'PD-010', '无锡市梁溪区清扬路 168 号', '唐琛', '09:00-23:00', 24, 0, 24, '运营中');

INSERT INTO biz_parking_region (id, parking_lot_id, region_name, region_type) VALUES
(1, 1, 'A区', '新能源车位区'),
(2, 1, 'B区', '普通车位区'),
(3, 2, 'A区', '新能源车位区'),
(4, 2, 'B区', '普通车位区'),
(5, 3, 'A区', '新能源车位区'),
(6, 3, 'B区', '普通车位区'),
(7, 4, 'A区', '新能源车位区'),
(8, 4, 'B区', '普通车位区'),
(9, 5, 'A区', '新能源车位区'),
(10, 5, 'B区', '普通车位区'),
(11, 6, 'A区', '新能源车位区'),
(12, 6, 'B区', '普通车位区'),
(13, 7, 'A区', '新能源车位区'),
(14, 7, 'B区', '普通车位区'),
(15, 8, 'A区', '新能源车位区'),
(16, 8, 'B区', '普通车位区'),
(17, 9, 'A区', '新能源车位区'),
(18, 9, 'B区', '普通车位区'),
(19, 10, 'A区', '新能源车位区'),
(20, 10, 'B区', '普通车位区');

INSERT INTO biz_pricing_rule (id, parking_lot_id, base_minutes, base_fee, hourly_fee, daily_cap) VALUES
(1, 1, 30, 0.00, 5.00, 45.00),
(2, 2, 30, 0.00, 4.00, 40.00),
(3, 3, 30, 0.00, 6.00, 48.00),
(4, 4, 30, 0.00, 8.00, 60.00),
(5, 5, 30, 0.00, 5.00, 42.00),
(6, 6, 30, 0.00, 6.00, 50.00),
(7, 7, 30, 0.00, 4.00, 35.00),
(8, 8, 30, 0.00, 7.00, 55.00),
(9, 9, 30, 0.00, 5.00, 44.00),
(10, 10, 30, 0.00, 5.00, 46.00);

INSERT INTO biz_parking_space (parking_lot_id, region_id, code, category, status)
WITH RECURSIVE seq AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM seq WHERE n < 36
)
SELECT
    lot.id,
    CASE
        WHEN seq.n <= CEIL(lot.total_spaces / 2.0) THEN region_a.id
        ELSE region_b.id
    END AS region_id,
    CONCAT(
        lot.code,
        '-',
        CASE WHEN seq.n <= CEIL(lot.total_spaces / 2.0) THEN 'A' ELSE 'B' END,
        '-',
        LPAD(seq.n, 3, '0')
    ) AS code,
    CASE
        WHEN seq.n <= CASE lot.id
            WHEN 1 THEN 4
            WHEN 2 THEN 3
            WHEN 3 THEN 4
            WHEN 4 THEN 6
            WHEN 5 THEN 4
            WHEN 6 THEN 5
            WHEN 7 THEN 3
            WHEN 8 THEN 3
            WHEN 9 THEN 5
            WHEN 10 THEN 4
        END THEN '新能源'
        ELSE '普通'
    END AS category,
    CASE
        WHEN CONCAT(
            lot.code,
            '-',
            CASE WHEN seq.n <= CEIL(lot.total_spaces / 2.0) THEN 'A' ELSE 'B' END,
            '-',
            LPAD(seq.n, 3, '0')
        ) IN (
            'PD-001-A-001','PD-002-A-001','PD-003-A-001','PD-004-A-001','PD-005-A-001',
            'PD-006-A-001','PD-007-A-001','PD-008-A-001','PD-009-A-001','PD-010-A-001'
        ) THEN '占用'
        ELSE '空闲'
    END AS status
FROM biz_parking_lot lot
JOIN biz_parking_region region_a ON region_a.parking_lot_id = lot.id AND region_a.region_name = 'A区'
JOIN biz_parking_region region_b ON region_b.parking_lot_id = lot.id AND region_b.region_name = 'B区'
JOIN seq ON seq.n <= lot.total_spaces;

INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 1, 1, 1, s.id, NOW() - INTERVAL 10 DAY - INTERVAL 4 HOUR, NOW() - INTERVAL 10 DAY - INTERVAL 2 HOUR, 120, 10.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-001-B-013';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 2, 2, 2, s.id, NOW() - INTERVAL 9 DAY - INTERVAL 5 HOUR, NOW() - INTERVAL 9 DAY - INTERVAL 3 HOUR, 120, 8.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-002-B-011';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 3, 1, 3, s.id, NOW() - INTERVAL 8 DAY - INTERVAL 6 HOUR, NOW() - INTERVAL 8 DAY - INTERVAL 4 HOUR, 130, 18.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-003-B-015';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 4, 2, 4, s.id, NOW() - INTERVAL 7 DAY - INTERVAL 6 HOUR, NOW() - INTERVAL 7 DAY - INTERVAL 4 HOUR, 145, 24.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-004-B-019';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 5, 1, 5, s.id, NOW() - INTERVAL 6 DAY - INTERVAL 5 HOUR, NOW() - INTERVAL 6 DAY - INTERVAL 3 HOUR, 120, 15.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-005-B-014';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 6, 2, 6, s.id, NOW() - INTERVAL 5 DAY - INTERVAL 5 HOUR, NOW() - INTERVAL 5 DAY - INTERVAL 3 HOUR, 125, 12.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-006-B-017';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 7, 1, 7, s.id, NOW() - INTERVAL 4 DAY - INTERVAL 4 HOUR, NOW() - INTERVAL 4 DAY - INTERVAL 2 HOUR, 110, 8.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-007-B-010';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 8, 2, 8, s.id, NOW() - INTERVAL 3 DAY - INTERVAL 5 HOUR, NOW() - INTERVAL 3 DAY - INTERVAL 3 HOUR, 175, 21.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-008-B-012';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 9, 1, 9, s.id, NOW() - INTERVAL 2 DAY - INTERVAL 4 HOUR, NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR, 130, 10.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-009-B-016';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 10, 2, 10, s.id, NOW() - INTERVAL 1 DAY - INTERVAL 4 HOUR, NOW() - INTERVAL 1 DAY - INTERVAL 2 HOUR, 145, 15.00, '已完成'
FROM biz_parking_space s WHERE s.code = 'PD-010-B-013';

INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 11, 1, 1, s.id, NOW() - INTERVAL 90 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-001-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 12, 2, 2, s.id, NOW() - INTERVAL 84 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-002-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 13, 1, 3, s.id, NOW() - INTERVAL 77 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-003-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 14, 2, 4, s.id, NOW() - INTERVAL 69 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-004-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 15, 1, 5, s.id, NOW() - INTERVAL 61 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-005-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 16, 2, 6, s.id, NOW() - INTERVAL 60 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-006-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 17, 1, 7, s.id, NOW() - INTERVAL 53 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-007-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 18, 2, 8, s.id, NOW() - INTERVAL 45 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-008-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 19, 1, 9, s.id, NOW() - INTERVAL 37 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-009-A-001';
INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
SELECT 20, 2, 10, s.id, NOW() - INTERVAL 28 MINUTE, NULL, 0, 0.00, '在场'
FROM biz_parking_space s WHERE s.code = 'PD-010-A-001';

INSERT INTO biz_order (id, record_id, amount, payment_status, created_at) VALUES
(1, 1, 10.00, '已支付', NOW() - INTERVAL 10 DAY - INTERVAL 2 HOUR),
(2, 2, 8.00, '已支付', NOW() - INTERVAL 9 DAY - INTERVAL 3 HOUR),
(3, 3, 18.00, '已支付', NOW() - INTERVAL 8 DAY - INTERVAL 4 HOUR),
(4, 4, 24.00, '已支付', NOW() - INTERVAL 7 DAY - INTERVAL 4 HOUR),
(5, 5, 15.00, '已支付', NOW() - INTERVAL 6 DAY - INTERVAL 3 HOUR),
(6, 6, 12.00, '已支付', NOW() - INTERVAL 5 DAY - INTERVAL 3 HOUR),
(7, 7, 8.00, '已支付', NOW() - INTERVAL 4 DAY - INTERVAL 2 HOUR),
(8, 8, 21.00, '已支付', NOW() - INTERVAL 3 DAY - INTERVAL 3 HOUR),
(9, 9, 10.00, '已支付', NOW() - INTERVAL 2 DAY - INTERVAL 2 HOUR),
(10, 10, 15.00, '已支付', NOW() - INTERVAL 1 DAY - INTERVAL 2 HOUR);

UPDATE biz_parking_lot lot
SET occupied_spaces = (
        SELECT COUNT(*)
        FROM biz_parking_space s
        WHERE s.parking_lot_id = lot.id AND s.status = '占用'
    ),
    free_spaces = (
        SELECT COUNT(*)
        FROM biz_parking_space s
        WHERE s.parking_lot_id = lot.id AND s.status = '空闲'
    );

INSERT INTO sys_notice (id, title, content, level_name) VALUES
(1, '毕业设计演示账号说明', '车主登录账号：owner / 123456，管理员通过首页“进入后台管理”按钮进入。', '重要'),
(2, '系统演示范围', '支持停车资源管理、车辆进出场、订单结算、数据看板、调度建议等核心流程。', '普通');

INSERT INTO sys_operation_log (id, action_name, detail_text, created_at) VALUES
(1, '初始化系统', '已完成 MySQL 演示数据装载，适合毕业设计答辩演示。', NOW() - INTERVAL 2 HOUR),
(2, '数据库接入', '系统已切换为真实 MySQL 数据源。', NOW() - INTERVAL 90 MINUTE),
(3, '完成模块联调', '管理端、用户端和核心业务流程已对齐。', NOW() - INTERVAL 30 MINUTE);
