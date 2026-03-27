package com.parking.system.service;

import com.parking.system.dto.CheckInRequest;
import com.parking.system.dto.LoginRequest;
import com.parking.system.dto.ParkingLotCreateRequest;
import com.parking.system.dto.RecordUpdateRequest;
import com.parking.system.dto.RegisterRequest;
import com.parking.system.dto.VehicleCreateRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParkingDemoService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;

    public ParkingDemoService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> login(LoginRequest request) {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList("""
                SELECT id, username, password, real_name, phone, role_name, status
                FROM sys_user
                WHERE username = ?
                """, request.username());
        if (rows.isEmpty()) {
            throw new IllegalArgumentException("账号不存在");
        }
        Map<String, Object> userRow = rows.get(0);
        if (!String.valueOf(userRow.get("password")).equals(request.password())) {
            throw new IllegalArgumentException("密码错误");
        }
        Map<String, Object> user = mapUserRow(userRow);
        return Map.of(
                "token", "mysql-token-" + request.username(),
                "user", user,
                "menus", buildMenus(String.valueOf(user.get("role")))
        );
    }

    @Transactional
    public Map<String, Object> registerOwner(RegisterRequest request) {
        Integer usernameCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_user WHERE username = ?",
                Integer.class,
                request.username()
        );
        if (usernameCount != null && usernameCount > 0) {
            throw new IllegalArgumentException("用户名已存在");
        }

        Long userId = insertAndReturnId("""
                INSERT INTO sys_user (username, password, real_name, phone, role_name, status)
                VALUES (?, ?, ?, ?, '车主', '启用')
                """, request.username(), request.password(), request.realName(), request.phone());

        jdbcTemplate.update("""
                INSERT INTO sys_user_role (user_id, role_id)
                VALUES (?, 3)
                """, userId);

        appendLog("车主注册", request.realName() + " 完成注册，账号 " + request.username());
        return jdbcTemplate.queryForList("""
                        SELECT id, username, real_name, phone, role_name, status
                        FROM sys_user
                        WHERE id = ?
                        """, userId)
                .stream()
                .findFirst()
                .map(this::mapUserRow)
                .orElseThrow(() -> new IllegalArgumentException("注册失败"));
    }

    public Map<String, Object> currentUser() {
        Map<String, Object> defaultAccount = mapUserRow(jdbcTemplate.queryForList("""
                        SELECT id, username, real_name, phone, role_name, status
                        FROM sys_user
                        ORDER BY id
                        LIMIT 1
                        """)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("系统用户不存在")));

        return Map.of(
                "projectName", "城市停车资源管理系统",
                "team", List.of(
                        Map.of("name", "陈一鸣", "role", "项目经理/系统设计师", "responsibility", "需求分析、UML 建模、项目计划书与答辩统筹"),
                        Map.of("name", "李嘉豪", "role", "后端与数据库工程师", "responsibility", "数据库设计、接口开发、业务逻辑、部署文档"),
                        Map.of("name", "王若溪", "role", "前端与测试工程师", "responsibility", "管理端与用户端页面、联调测试、演示视频素材")
                ),
                "defaultAccount", defaultAccount
        );
    }

    public List<Map<String, Object>> menus(String role) {
        return buildMenus(role);
    }

    public List<Map<String, Object>> listUsers() {
        return jdbcTemplate.queryForList("""
                        SELECT id, username, real_name, phone, role_name, status
                        FROM sys_user
                        ORDER BY id
                        """)
                .stream()
                .map(this::mapUserRow)
                .toList();
    }

    public List<Map<String, Object>> listVehicles() {
        return jdbcTemplate.queryForList("""
                        SELECT v.id, v.user_id AS userId, u.real_name AS ownerName, v.plate_number AS plateNumber,
                               v.brand, v.color, v.status
                        FROM biz_vehicle v
                        JOIN sys_user u ON u.id = v.user_id
                        ORDER BY v.id
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createVehicle(VehicleCreateRequest request) {
        Map<String, Object> owner = jdbcTemplate.queryForList("""
                        SELECT id, real_name
                        FROM sys_user
                        WHERE id = ?
                        """, request.userId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));

        Long createdId = insertAndReturnId("""
                INSERT INTO biz_vehicle (user_id, plate_number, brand, color, status)
                VALUES (?, ?, ?, ?, '正常')
                """, request.userId(), request.plateNumber().toUpperCase(), request.brand(), request.color());

        appendLog("新增车辆档案", "车主 " + owner.get("real_name") + " 新增车辆 " + request.plateNumber().toUpperCase());
        return jdbcTemplate.queryForList("""
                        SELECT v.id, v.user_id AS userId, u.real_name AS ownerName, v.plate_number AS plateNumber,
                               v.brand, v.color, v.status
                        FROM biz_vehicle v
                        JOIN sys_user u ON u.id = v.user_id
                        WHERE v.id = ?
                        """, createdId)
                .stream()
                .findFirst()
                .map(this::normalizeNumericMap)
                .orElseThrow(() -> new IllegalArgumentException("车辆新增失败"));
    }

    @Transactional
    public void deleteVehicle(Long currentVehicleId) {
        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM biz_parking_record
                WHERE vehicle_id = ? AND status = '在场'
                """, Integer.class, currentVehicleId);
        if (activeCount != null && activeCount > 0) {
            throw new IllegalArgumentException("该车辆存在在场记录，不能删除");
        }

        String plateNumber = jdbcTemplate.queryForList("""
                        SELECT plate_number
                        FROM biz_vehicle
                        WHERE id = ?
                        """, currentVehicleId)
                .stream()
                .findFirst()
                .map(row -> String.valueOf(row.get("plate_number")))
                .orElseThrow(() -> new IllegalArgumentException("车辆不存在"));

        jdbcTemplate.update("DELETE FROM biz_vehicle WHERE id = ?", currentVehicleId);
        appendLog("删除车辆档案", "删除车辆 " + plateNumber);
    }

    public List<Map<String, Object>> listParkingLots() {
        refreshParkingLotMetrics();
        return jdbcTemplate.queryForList("""
                        SELECT id, name, code, address, manager_name AS managerName, business_hours AS businessHours,
                               total_spaces AS totalSpaces, occupied_spaces AS occupiedSpaces, free_spaces AS freeSpaces, status
                        FROM biz_parking_lot
                        ORDER BY id
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> createParkingLot(ParkingLotCreateRequest request) {
        Long createdLotId = insertAndReturnId("""
                INSERT INTO biz_parking_lot (name, code, address, manager_name, business_hours, total_spaces, occupied_spaces, free_spaces, status)
                VALUES (?, ?, ?, '系统新增', ?, ?, 0, ?, '运营中')
                """, request.name(), request.code(), request.address(), request.businessHours(), request.totalSpaces(), request.totalSpaces());

        Long regionAId = insertAndReturnId("""
                INSERT INTO biz_parking_region (parking_lot_id, region_name, region_type)
                VALUES (?, 'A区', '新能源车位区')
                """, createdLotId);
        Long regionBId = insertAndReturnId("""
                INSERT INTO biz_parking_region (parking_lot_id, region_name, region_type)
                VALUES (?, 'B区', '普通车位区')
                """, createdLotId);

        int newEnergyCount = Math.max(4, (int) Math.ceil(request.totalSpaces() * 0.18));
        for (int i = 1; i <= request.totalSpaces(); i++) {
            boolean isRegionA = i <= (int) Math.ceil(request.totalSpaces() / 2.0);
            jdbcTemplate.update("""
                    INSERT INTO biz_parking_space (parking_lot_id, region_id, code, category, status)
                    VALUES (?, ?, ?, ?, '空闲')
                    """,
                    createdLotId,
                    isRegionA ? regionAId : regionBId,
                    buildSpaceCode(request.code(), i, request.totalSpaces()),
                    i <= newEnergyCount ? "新能源" : "普通");
        }

        jdbcTemplate.update("""
                INSERT INTO biz_pricing_rule (parking_lot_id, base_minutes, base_fee, hourly_fee, daily_cap)
                VALUES (?, 30, 0.00, 4.00, 38.00)
                """, createdLotId);

        appendLog("新增停车场", "新增停车场 " + request.name() + "，车位 " + request.totalSpaces() + " 个");
        refreshParkingLotMetrics();
        return jdbcTemplate.queryForList("""
                        SELECT id, name, code, address, manager_name AS managerName, business_hours AS businessHours,
                               total_spaces AS totalSpaces, occupied_spaces AS occupiedSpaces, free_spaces AS freeSpaces, status
                        FROM biz_parking_lot
                        WHERE id = ?
                        """, createdLotId)
                .stream()
                .findFirst()
                .map(this::normalizeNumericMap)
                .orElseThrow(() -> new IllegalArgumentException("停车场创建失败"));
    }

    public List<Map<String, Object>> listSpaces() {
        return jdbcTemplate.queryForList("""
                        SELECT s.id, s.parking_lot_id AS parkingLotId, s.code,
                               COALESCE(r.region_name, CASE WHEN s.code LIKE '%-A-%' THEN 'A区' ELSE 'B区' END) AS regionName,
                               s.category, s.status
                        FROM biz_parking_space s
                        LEFT JOIN biz_parking_region r ON r.id = s.region_id
                        ORDER BY s.code
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    public List<Map<String, Object>> listRegions() {
        List<Map<String, Object>> lots = listParkingLots();
        List<Map<String, Object>> regions = new ArrayList<>();
        for (Map<String, Object> lot : lots) {
            Long lotId = toLong(lot.get("id"));
            int aCount = countSpacesByRegion(lotId, "A区", "空闲");
            int bCount = countSpacesByRegion(lotId, "B区", "空闲");
            regions.add(Map.of(
                    "parkingLotId", lotId,
                    "parkingLotName", lot.get("name"),
                    "regions", List.of(
                            Map.of("name", "A区", "type", "新能源车位区", "available", aCount),
                            Map.of("name", "B区", "type", "普通车位区", "available", bCount)
                    )
            ));
        }
        return regions;
    }

    public List<Map<String, Object>> listPricingRules() {
        return jdbcTemplate.queryForList("""
                        SELECT p.id, p.parking_lot_id AS parkingLotId, l.name AS parkingLotName,
                               p.base_minutes AS baseMinutes, p.base_fee AS baseFee,
                               p.hourly_fee AS hourlyFee, p.daily_cap AS dailyCap
                        FROM biz_pricing_rule p
                        JOIN biz_parking_lot l ON l.id = p.parking_lot_id
                        ORDER BY p.parking_lot_id
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    public List<Map<String, Object>> listRecords() {
        return jdbcTemplate.queryForList("""
                        SELECT r.id, r.vehicle_id AS vehicleId, v.plate_number AS plateNumber, u.real_name AS ownerName,
                               r.parking_lot_id AS parkingLotId, l.name AS parkingLotName, s.code AS spaceCode,
                               DATE_FORMAT(r.entry_time, '%Y-%m-%d %H:%i:%s') AS entryTime,
                               CASE WHEN r.exit_time IS NULL THEN NULL ELSE DATE_FORMAT(r.exit_time, '%Y-%m-%d %H:%i:%s') END AS exitTime,
                               r.duration_minutes AS durationMinutes, r.amount, r.status
                        FROM biz_parking_record r
                        JOIN biz_vehicle v ON v.id = r.vehicle_id
                        JOIN sys_user u ON u.id = v.user_id
                        JOIN biz_parking_lot l ON l.id = r.parking_lot_id
                        LEFT JOIN biz_parking_space s ON s.id = r.parking_space_id
                        ORDER BY r.entry_time DESC
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .map(this::decorateRecordForResponse)
                .toList();
    }

    public List<Map<String, Object>> listOrders() {
        return jdbcTemplate.queryForList("""
                        SELECT o.id, o.record_id AS recordId, v.plate_number AS plateNumber,
                               l.name AS parkingLotName, o.amount, o.payment_status AS paymentStatus,
                               DATE_FORMAT(o.created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                        FROM biz_order o
                        JOIN biz_parking_record r ON r.id = o.record_id
                        JOIN biz_vehicle v ON v.id = r.vehicle_id
                        JOIN biz_parking_lot l ON l.id = r.parking_lot_id
                        ORDER BY o.created_at DESC
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    @Transactional
    public Map<String, Object> checkIn(CheckInRequest request) {
        Integer activeCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM biz_parking_record
                WHERE vehicle_id = ? AND status = '在场'
                """, Integer.class, request.vehicleId());
        if (activeCount != null && activeCount > 0) {
            throw new IllegalArgumentException("该车辆已在场内，不能重复入场");
        }

        Map<String, Object> freeSpace = jdbcTemplate.queryForList("""
                        SELECT s.id, s.code, l.name AS parkingLotName
                        FROM biz_parking_space s
                        JOIN biz_parking_lot l ON l.id = s.parking_lot_id
                        WHERE s.parking_lot_id = ? AND s.status = '空闲'
                        ORDER BY s.id
                        LIMIT 1
                        """, request.parkingLotId())
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前停车场暂无空闲车位"));

        jdbcTemplate.update("UPDATE biz_parking_space SET status = '占用' WHERE id = ?", freeSpace.get("id"));
        LocalDateTime entryTime = LocalDateTime.now().minusMinutes(15);

        Long createdRecordId = insertAndReturnId("""
                INSERT INTO biz_parking_record (vehicle_id, parking_lot_id, parking_space_id, entry_time, exit_time, duration_minutes, amount, status)
                VALUES (?, ?, ?, ?, NULL, 0, 0.00, '在场')
                """, request.vehicleId(), request.parkingLotId(), freeSpace.get("id"), entryTime);

        appendLog("车辆入场", queryPlateNumber(request.vehicleId()) + " 进入 " + freeSpace.get("parkingLotName") + "，车位 " + freeSpace.get("code"));
        refreshParkingLotMetrics();
        return listRecords().stream()
                .filter(item -> toLong(item.get("id")).equals(createdRecordId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("入场记录创建失败"));
    }

    @Transactional
    public Map<String, Object> checkOut(Long currentRecordId) {
        Map<String, Object> record = loadRawRecord(currentRecordId);
        if (!"在场".equals(String.valueOf(record.get("status")))) {
            throw new IllegalArgumentException("该停车记录已出场");
        }

        LocalDateTime entryTime = LocalDateTime.parse(String.valueOf(record.get("entryTime")), FORMATTER);
        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Math.max(0, Duration.between(entryTime, exitTime).toMinutes());
        BigDecimal amount = calculateAmount(toLong(record.get("parkingLotId")), minutes);

        jdbcTemplate.update("""
                UPDATE biz_parking_record
                SET exit_time = ?, duration_minutes = ?, amount = ?, status = '已完成'
                WHERE id = ?
                """, exitTime, minutes, amount, currentRecordId);
        jdbcTemplate.update("""
                UPDATE biz_parking_space
                SET status = '空闲'
                WHERE id = (SELECT parking_space_id FROM biz_parking_record WHERE id = ?)
                """, currentRecordId);
        jdbcTemplate.update("""
                INSERT INTO biz_order (record_id, amount, payment_status, created_at)
                VALUES (?, ?, '未支付', ?)
                """, currentRecordId, amount, exitTime);

        appendLog("车辆出场", record.get("plateNumber") + " 离开 " + record.get("parkingLotName") + "，待支付 " + amount + " 元");
        refreshParkingLotMetrics();

        Map<String, Object> refreshedRecord = listRecords().stream()
                .filter(item -> toLong(item.get("id")).equals(currentRecordId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("停车记录不存在"));
        Map<String, Object> order = listOrders().stream()
                .filter(item -> toLong(item.get("recordId")).equals(currentRecordId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
        return Map.of("record", refreshedRecord, "order", order);
    }

    @Transactional
    public Map<String, Object> updateRecord(Long currentRecordId, RecordUpdateRequest request) {
        Map<String, Object> record = loadRawRecord(currentRecordId);
        LocalDateTime entryTime = LocalDateTime.parse(request.entryTime(), FORMATTER);
        String exitTimeText = request.exitTime() == null ? "" : request.exitTime().trim();

        if (exitTimeText.isEmpty()) {
            jdbcTemplate.update("""
                    UPDATE biz_parking_record
                    SET entry_time = ?, exit_time = NULL, duration_minutes = 0, amount = 0.00, status = '在场'
                    WHERE id = ?
                    """, entryTime, currentRecordId);
            jdbcTemplate.update("""
                    UPDATE biz_parking_space
                    SET status = '占用'
                    WHERE id = (SELECT parking_space_id FROM biz_parking_record WHERE id = ?)
                    """, currentRecordId);
            jdbcTemplate.update("DELETE FROM biz_order WHERE record_id = ?", currentRecordId);
        } else {
            LocalDateTime exitTime = LocalDateTime.parse(exitTimeText, FORMATTER);
            long minutes = Math.max(0, Duration.between(entryTime, exitTime).toMinutes());
            BigDecimal amount = request.amount().setScale(2, RoundingMode.HALF_UP);

            jdbcTemplate.update("""
                    UPDATE biz_parking_record
                    SET entry_time = ?, exit_time = ?, duration_minutes = ?, amount = ?, status = '已完成'
                    WHERE id = ?
                    """, entryTime, exitTime, minutes, amount, currentRecordId);
            jdbcTemplate.update("""
                    UPDATE biz_parking_space
                    SET status = '空闲'
                    WHERE id = (SELECT parking_space_id FROM biz_parking_record WHERE id = ?)
                    """, currentRecordId);

            Integer existingOrder = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM biz_order WHERE record_id = ?",
                    Integer.class,
                    currentRecordId
            );
            if (existingOrder != null && existingOrder > 0) {
                jdbcTemplate.update("""
                        UPDATE biz_order
                        SET amount = ?, payment_status = ?, created_at = ?
                        WHERE record_id = ?
                        """, amount, request.paymentStatus(), exitTime, currentRecordId);
            } else {
                jdbcTemplate.update("""
                        INSERT INTO biz_order (record_id, amount, payment_status, created_at)
                        VALUES (?, ?, ?, ?)
                        """, currentRecordId, amount, request.paymentStatus(), exitTime);
            }
        }

        appendLog("编辑停车记录", record.get("plateNumber") + " 的停车记录已更新");
        refreshParkingLotMetrics();
        return listRecords().stream()
                .filter(item -> toLong(item.get("id")).equals(currentRecordId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("停车记录不存在"));
    }

    @Transactional
    public Map<String, Object> payOrder(Long currentRecordId) {
        Map<String, Object> record = loadRawRecord(currentRecordId);
        Integer existingOrder = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_order WHERE record_id = ?",
                Integer.class,
                currentRecordId
        );
        if (existingOrder == null || existingOrder == 0) {
            throw new IllegalArgumentException("订单不存在");
        }

        jdbcTemplate.update("""
                UPDATE biz_order
                SET payment_status = '已支付', created_at = ?
                WHERE record_id = ?
                """, LocalDateTime.now(), currentRecordId);
        appendLog("订单支付", record.get("plateNumber") + " 完成支付，订单记录 " + currentRecordId);
        return listOrders().stream()
                .filter(item -> toLong(item.get("recordId")).equals(currentRecordId))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("订单不存在"));
    }

    public Map<String, Object> dashboardOverview() {
        refreshParkingLotMetrics();
        int totalSpaces = queryCount("SELECT COUNT(*) FROM biz_parking_space");
        int freeSpaces = queryCount("SELECT COUNT(*) FROM biz_parking_space WHERE status = '空闲'");
        int occupiedSpaces = totalSpaces - freeSpaces;
        BigDecimal totalRevenue = jdbcTemplate.queryForObject("""
                SELECT COALESCE(SUM(amount), 0.00)
                FROM biz_order
                WHERE payment_status = '已支付'
                """, BigDecimal.class);
        if (totalRevenue == null) {
            totalRevenue = BigDecimal.ZERO;
        }

        List<Map<String, Object>> topParkingLots = jdbcTemplate.queryForList("""
                        SELECT name, occupied_spaces AS occupiedSpaces, total_spaces AS totalSpaces, free_spaces AS freeSpaces
                        FROM biz_parking_lot
                        ORDER BY occupied_spaces DESC, id
                        """)
                .stream()
                .map(row -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("name", String.valueOf(row.get("name")));
                    item.put("occupancyRate", percentage(toInt(row.get("occupiedSpaces")), toInt(row.get("totalSpaces"))));
                    item.put("freeSpaces", toInt(row.get("freeSpaces")));
                    return item;
                })
                .toList();

        return Map.of(
                "metrics", List.of(
                        metric("停车场总数", queryCount("SELECT COUNT(*) FROM biz_parking_lot"), "管理全市核心停车资源"),
                        metric("车位总数", totalSpaces, "覆盖普通与新能源车位"),
                        metric("当前空闲车位", freeSpaces, "支持资源调度"),
                        metric("累计收入", totalRevenue.setScale(2, RoundingMode.HALF_UP) + " 元", "基于已支付订单统计")
                ),
                "occupancyRate", percentage(occupiedSpaces, totalSpaces),
                "parkingTrend", parkingTrend(),
                "topParkingLots", topParkingLots
        );
    }

    public List<Map<String, Object>> dispatchSuggestions() {
        refreshParkingLotMetrics();
        return jdbcTemplate.queryForList("""
                        SELECT name, occupied_spaces AS occupiedSpaces, total_spaces AS totalSpaces
                        FROM biz_parking_lot
                        ORDER BY id
                        """)
                .stream()
                .map(row -> {
                    int occupied = toInt(row.get("occupiedSpaces"));
                    int total = toInt(row.get("totalSpaces"));
                    double rate = total == 0 ? 0 : occupied * 100.0 / total;
                    String level = rate >= 85 ? "高" : rate >= 65 ? "中" : "低";
                    String action = rate >= 85
                            ? "建议引导车辆分流至周边停车场，并增加现场调度人员。"
                            : rate >= 65
                            ? "建议开启动态引导屏，优先分配新能源和临停车位。"
                            : "当前资源充足，可用于分担周边高峰压力。";
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("parkingLotName", String.valueOf(row.get("name")));
                    item.put("occupancyRate", percentage(occupied, total));
                    item.put("warningLevel", level);
                    item.put("action", action);
                    return item;
                })
                .toList();
    }

    public List<Map<String, Object>> listNotices() {
        return jdbcTemplate.queryForList("""
                        SELECT id, title, content, level_name AS level
                        FROM sys_notice
                        ORDER BY id
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    public List<Map<String, Object>> listLogs() {
        return jdbcTemplate.queryForList("""
                        SELECT id, action_name AS action, detail_text AS detail,
                               DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS createdAt
                        FROM sys_operation_log
                        ORDER BY created_at DESC, id DESC
                        """)
                .stream()
                .map(this::normalizeNumericMap)
                .toList();
    }

    private Map<String, Object> loadRawRecord(Long recordId) {
        return jdbcTemplate.queryForList("""
                        SELECT r.id, r.vehicle_id AS vehicleId, v.plate_number AS plateNumber, u.real_name AS ownerName,
                               r.parking_lot_id AS parkingLotId, l.name AS parkingLotName, s.code AS spaceCode,
                               DATE_FORMAT(r.entry_time, '%Y-%m-%d %H:%i:%s') AS entryTime,
                               CASE WHEN r.exit_time IS NULL THEN NULL ELSE DATE_FORMAT(r.exit_time, '%Y-%m-%d %H:%i:%s') END AS exitTime,
                               r.duration_minutes AS durationMinutes, r.amount, r.status
                        FROM biz_parking_record r
                        JOIN biz_vehicle v ON v.id = r.vehicle_id
                        JOIN sys_user u ON u.id = v.user_id
                        JOIN biz_parking_lot l ON l.id = r.parking_lot_id
                        LEFT JOIN biz_parking_space s ON s.id = r.parking_space_id
                        WHERE r.id = ?
                        """, recordId)
                .stream()
                .findFirst()
                .map(this::normalizeNumericMap)
                .orElseThrow(() -> new IllegalArgumentException("停车记录不存在"));
    }

    private Map<String, Object> decorateRecordForResponse(Map<String, Object> source) {
        Map<String, Object> record = new LinkedHashMap<>(source);
        if (!"在场".equals(String.valueOf(source.get("status")))) {
            record.put("amount", toBigDecimal(source.get("amount")));
            return record;
        }

        LocalDateTime entryTime = LocalDateTime.parse(String.valueOf(source.get("entryTime")), FORMATTER);
        long minutes = Math.max(0, Duration.between(entryTime, LocalDateTime.now()).toMinutes());
        BigDecimal amount = calculateAmount(toLong(source.get("parkingLotId")), minutes);
        record.put("durationMinutes", minutes);
        record.put("amount", amount);
        return record;
    }

    private BigDecimal calculateAmount(Long lotId, long minutes) {
        Map<String, Object> rule = jdbcTemplate.queryForList("""
                        SELECT base_minutes AS baseMinutes, base_fee AS baseFee, hourly_fee AS hourlyFee, daily_cap AS dailyCap
                        FROM biz_pricing_rule
                        WHERE parking_lot_id = ?
                        """, lotId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到收费规则"));

        long billableMinutes = Math.max(toLong(rule.get("baseMinutes")), minutes);
        BigDecimal baseFee = toBigDecimal(rule.get("baseFee"));
        BigDecimal hourlyFee = toBigDecimal(rule.get("hourlyFee"));
        BigDecimal dailyCap = toBigDecimal(rule.get("dailyCap"));
        BigDecimal hours = BigDecimal.valueOf(Math.ceil(billableMinutes / 60.0));
        return baseFee.add(hourlyFee.multiply(hours)).min(dailyCap).setScale(2, RoundingMode.HALF_UP);
    }

    private void appendLog(String action, String detail) {
        jdbcTemplate.update("""
                INSERT INTO sys_operation_log (action_name, detail_text, created_at)
                VALUES (?, ?, ?)
                """, action, detail, LocalDateTime.now());
    }

    private void refreshParkingLotMetrics() {
        jdbcTemplate.update("""
                UPDATE biz_parking_lot lot
                SET occupied_spaces = (
                        SELECT COUNT(*)
                        FROM biz_parking_space space
                        WHERE space.parking_lot_id = lot.id AND space.status = '占用'
                    ),
                    free_spaces = (
                        SELECT COUNT(*)
                        FROM biz_parking_space space
                        WHERE space.parking_lot_id = lot.id AND space.status = '空闲'
                    ),
                    total_spaces = (
                        SELECT COUNT(*)
                        FROM biz_parking_space space
                        WHERE space.parking_lot_id = lot.id
                    )
                """);
    }

    private int countSpacesByRegion(Long lotId, String regionName, String status) {
        Integer count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM biz_parking_space s
                LEFT JOIN biz_parking_region r ON r.id = s.region_id
                WHERE s.parking_lot_id = ?
                  AND COALESCE(r.region_name, CASE WHEN s.code LIKE '%-A-%' THEN 'A区' ELSE 'B区' END) = ?
                  AND s.status = ?
                """, Integer.class, lotId, regionName, status);
        return count == null ? 0 : count;
    }

    private String queryPlateNumber(Long vehicleId) {
        return jdbcTemplate.queryForObject("SELECT plate_number FROM biz_vehicle WHERE id = ?", String.class, vehicleId);
    }

    private Long insertAndReturnId(String sql, Object... args) {
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(connection -> {
            PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            for (int i = 0; i < args.length; i++) {
                statement.setObject(i + 1, args[i]);
            }
            return statement;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("生成主键失败");
        }
        return key.longValue();
    }

    private Map<String, Object> mapUserRow(Map<String, Object> row) {
        return Map.of(
                "id", toLong(row.get("id")),
                "username", String.valueOf(row.get("username")),
                "realName", String.valueOf(row.get("real_name")),
                "role", String.valueOf(row.get("role_name")),
                "responsibility", responsibilityOf(String.valueOf(row.get("role_name"))),
                "phone", String.valueOf(row.get("phone")),
                "status", String.valueOf(row.get("status"))
        );
    }

    private String responsibilityOf(String role) {
        return switch (role) {
            case "管理员" -> "负责系统配置、资源管理和全局监控";
            case "停车场管理员" -> "负责停车场资源维护与订单处理";
            case "车主" -> "负责停车查询、车辆管理、停车缴费与订单查看";
            default -> "负责系统演示与业务协同";
        };
    }

    private Map<String, Object> metric(String title, Object value, String description) {
        return Map.of("title", title, "value", value, "description", description);
    }

    private List<Map<String, Object>> parkingTrend() {
        return List.of(
                trend("08:00", 12, 7),
                trend("10:00", 18, 14),
                trend("12:00", 23, 18),
                trend("14:00", 28, 22),
                trend("16:00", 24, 19),
                trend("18:00", 30, 26),
                trend("20:00", 17, 9)
        );
    }

    private Map<String, Object> trend(String time, int entries, int exits) {
        return Map.of("time", time, "entries", entries, "exits", exits);
    }

    private int queryCount(String sql, Object... args) {
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, args);
        return count == null ? 0 : count;
    }

    private String percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return "0%";
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(1, RoundingMode.HALF_UP) + "%";
    }

    private String buildSpaceCode(String lotCode, int index, int totalSpaces) {
        String region = index <= (int) Math.ceil(totalSpaces / 2.0) ? "A" : "B";
        return lotCode + "-" + region + "-" + String.format("%03d", index);
    }

    private Long toLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(String.valueOf(value));
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.setScale(2, RoundingMode.HALF_UP);
        }
        if (value instanceof Number number) {
            return BigDecimal.valueOf(number.doubleValue()).setScale(2, RoundingMode.HALF_UP);
        }
        return new BigDecimal(String.valueOf(value)).setScale(2, RoundingMode.HALF_UP);
    }

    private Map<String, Object> normalizeNumericMap(Map<String, Object> raw) {
        Map<String, Object> normalized = new LinkedHashMap<>();
        raw.forEach((key, value) -> {
            if (value instanceof BigDecimal bigDecimal) {
                normalized.put(key, bigDecimal.setScale(2, RoundingMode.HALF_UP));
            } else {
                normalized.put(key, value);
            }
        });
        return normalized;
    }

    private List<Map<String, Object>> buildMenus(String role) {
        List<Map<String, Object>> common = new ArrayList<>(List.of(
                menu("dashboard", "综合看板"),
                menu("parking", "停车资源"),
                menu("records", "停车记录"),
                menu("system", "公告日志")
        ));
        if ("车主".equals(role)) {
            return List.of(
                    menu("dashboard", "停车概览"),
                    menu("user", "我的车辆"),
                    menu("records", "我的停车记录"),
                    menu("system", "系统公告")
            );
        }
        common.add(1, menu("users", "用户车辆"));
        return common;
    }

    private Map<String, Object> menu(String key, String title) {
        return Map.of("key", key, "title", title);
    }
}
