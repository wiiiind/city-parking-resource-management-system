USE city_parking_resource_management;

INSERT INTO sys_role (id, role_code, role_name, description) VALUES
(1, 'ADMIN', '管理员', '负责系统配置、资源管理和全局监控'),
(2, 'LOT_MANAGER', '停车场管理员', '负责停车场资源维护与订单处理'),
(3, 'OWNER', '车主', '负责车辆维护、停车查询和订单查看');

INSERT INTO sys_user (id, username, password, real_name, phone, role_name) VALUES
(1, 'admin', '123456', '系统管理员', '13800000001', '管理员'),
(2, 'manager', '123456', '停车场主管', '13800000002', '停车场管理员'),
(3, 'owner', '123456', '张晓彤', '13800000003', '车主');

INSERT INTO sys_user_role (user_id, role_id) VALUES
(1, 1), (2, 2), (3, 3);

INSERT INTO biz_vehicle (id, user_id, plate_number, brand, color, status) VALUES
(1, 3, '沪A12345', '比亚迪海豹', '白色', '正常'),
(2, 3, '沪B66218', '特斯拉Model 3', '灰色', '正常'),
(3, 2, '沪C90871', '大众帕萨特', '黑色', '正常');

INSERT INTO biz_parking_lot (id, name, code, address, manager_name, business_hours, total_spaces, occupied_spaces, free_spaces, status) VALUES
(1, '人民广场地下停车场', 'PD-001', '上海市黄浦区人民大道 88 号', '徐晨', '06:00-23:30', 24, 3, 21, '运营中'),
(2, '滨江商务停车中心', 'PD-002', '上海市浦东新区滨江大道 166 号', '高宇', '全天开放', 20, 2, 18, '运营中');

INSERT INTO biz_parking_region (id, parking_lot_id, region_name, region_type) VALUES
(1, 1, 'A区', '新能源车位区'),
(2, 1, 'B区', '普通车位区'),
(3, 2, 'A区', '新能源车位区'),
(4, 2, 'B区', '普通车位区');

INSERT INTO biz_parking_space (id, parking_lot_id, region_id, code, category, status) VALUES
(1, 1, 1, 'PD-001-A-001', '新能源', '占用'),
(2, 1, 1, 'PD-001-A-002', '新能源', '占用'),
(3, 1, 1, 'PD-001-A-003', '新能源', '占用'),
(4, 1, 1, 'PD-001-A-004', '新能源', '空闲'),
(5, 1, 2, 'PD-001-B-011', '普通', '空闲'),
(6, 2, 3, 'PD-002-A-001', '新能源', '占用'),
(7, 2, 3, 'PD-002-A-002', '新能源', '占用'),
(8, 2, 4, 'PD-002-B-014', '普通', '空闲');

INSERT INTO biz_pricing_rule (id, parking_lot_id, base_minutes, base_fee, hourly_fee, daily_cap) VALUES
(1, 1, 30, 0.00, 5.00, 45.00),
(2, 2, 30, 0.00, 4.00, 40.00);

INSERT INTO biz_parking_record (id, vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status) VALUES
(1, 1, 1, 1, '2026-03-10 08:00:00', '2026-03-10 10:00:00', 120, 10.00, '已完成'),
(2, 2, 2, 6, '2026-03-10 11:00:00', NULL, 0, 0.00, '在场');

INSERT INTO biz_order (id, record_id, amount, payment_status, created_at) VALUES
(1, 1, 10.00, '已支付', '2026-03-10 10:00:00');

INSERT INTO sys_notice (id, title, content, level_name) VALUES
(1, '演示账号说明', '管理员：admin / 123456；停车场管理员：manager / 123456；车主：owner / 123456', '重要'),
(2, '系统演示范围', '支持资源管理、车辆进出场、订单结算、数据看板和调度建议等核心流程。', '普通');

INSERT INTO sys_operation_log (id, action_name, detail_text, created_at) VALUES
(1, '初始化系统', '已完成示例数据装载，适合毕业设计答辩演示。', '2026-03-10 09:00:00'),
(2, '生成团队包装', '项目已按 3 人团队分工完成材料口径整理。', '2026-03-10 09:05:00'),
(3, '模块联调', '管理端、用户端和停车业务闭环已经打通。', '2026-03-10 09:10:00');
