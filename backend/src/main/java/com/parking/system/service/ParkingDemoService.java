package com.parking.system.service;

import com.parking.system.dto.CheckInRequest;
import com.parking.system.dto.LoginRequest;
import com.parking.system.dto.ParkingLotCreateRequest;
import com.parking.system.dto.RegisterRequest;
import com.parking.system.dto.VehicleCreateRequest;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ParkingDemoService {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final AtomicLong userId = new AtomicLong(3);
    private final AtomicLong vehicleId = new AtomicLong(4);
    private final AtomicLong parkingLotId = new AtomicLong(2);
    private final AtomicLong recordId = new AtomicLong(2);
    private final AtomicLong orderId = new AtomicLong(1);
    private final AtomicLong logId = new AtomicLong(3);

    private final List<Map<String, Object>> users = new ArrayList<>();
    private final List<Map<String, Object>> vehicles = new ArrayList<>();
    private final List<Map<String, Object>> parkingLots = new ArrayList<>();
    private final List<Map<String, Object>> spaces = new ArrayList<>();
    private final List<Map<String, Object>> pricingRules = new ArrayList<>();
    private final List<Map<String, Object>> parkingRecords = new ArrayList<>();
    private final List<Map<String, Object>> orders = new ArrayList<>();
    private final List<Map<String, Object>> notices = new ArrayList<>();
    private final List<Map<String, Object>> operationLogs = new ArrayList<>();
    private final Map<String, String> credentials = new HashMap<>();

    @PostConstruct
    public void init() {
        seedUsers();
        seedVehicles();
        seedParking();
        seedBusinessData();
        seedSystemData();
    }

    public synchronized Map<String, Object> login(LoginRequest request) {
        Map<String, Object> user = users.stream()
                .filter(item -> item.get("username").equals(request.username()))
                .findFirst()
                .orElse(null);
        if (user == null) {
            throw new IllegalArgumentException("账号不存在");
        }
        String password = credentials.get(request.username());
        if (password == null || !password.equals(request.password())) {
            throw new IllegalArgumentException("密码错误");
        }
        String role = String.valueOf(user.get("role"));
        return Map.of(
                "token", "demo-token-" + request.username(),
                "user", user,
                "menus", buildMenus(role)
        );
    }

    public synchronized Map<String, Object> registerOwner(RegisterRequest request) {
        boolean usernameExists = users.stream()
                .anyMatch(item -> String.valueOf(item.get("username")).equals(request.username()));
        if (usernameExists) {
            throw new IllegalArgumentException("用户名已存在");
        }
        Map<String, Object> user = user(
                userId.incrementAndGet(),
                request.username(),
                request.realName(),
                "车主",
                "负责停车查询、车辆管理、停车缴费与订单查看",
                request.phone()
        );
        users.add(user);
        credentials.put(request.username(), request.password());
        appendLog("车主注册", request.realName() + " 完成注册，账号 " + request.username());
        return user;
    }

    public synchronized Map<String, Object> currentUser() {
        return Map.of(
                "projectName", "城市停车资源管理系统",
                "team", List.of(
                        Map.of("name", "陈一鸣", "role", "项目经理/系统设计师", "responsibility", "需求分析、UML 建模、项目计划书与答辩统筹"),
                        Map.of("name", "李嘉豪", "role", "后端与数据库工程师", "responsibility", "数据库设计、接口开发、业务逻辑、部署文档"),
                        Map.of("name", "王若溪", "role", "前端与测试工程师", "responsibility", "管理端与用户端页面、联调测试、演示视频素材")
                ),
                "defaultAccount", users.get(0)
        );
    }

    public synchronized List<Map<String, Object>> menus(String role) {
        return buildMenus(role);
    }

    public synchronized List<Map<String, Object>> listUsers() {
        return users;
    }

    public synchronized List<Map<String, Object>> listVehicles() {
        return vehicles;
    }

    public synchronized Map<String, Object> createVehicle(VehicleCreateRequest request) {
        Map<String, Object> owner = getById(users, request.userId(), "用户不存在");
        Map<String, Object> vehicle = new LinkedHashMap<>();
        vehicle.put("id", vehicleId.incrementAndGet());
        vehicle.put("userId", request.userId());
        vehicle.put("ownerName", owner.get("realName"));
        vehicle.put("plateNumber", request.plateNumber().toUpperCase());
        vehicle.put("brand", request.brand());
        vehicle.put("color", request.color());
        vehicle.put("status", "正常");
        vehicles.add(vehicle);
        appendLog("新增车辆档案", "车主 " + owner.get("realName") + " 新增车辆 " + request.plateNumber());
        return vehicle;
    }

    public synchronized void deleteVehicle(Long currentVehicleId) {
        Map<String, Object> vehicle = getById(vehicles, currentVehicleId, "车辆不存在");
        boolean activeRecordExists = parkingRecords.stream()
                .anyMatch(item -> item.get("vehicleId").equals(currentVehicleId) && "在场".equals(item.get("status")));
        if (activeRecordExists) {
            throw new IllegalArgumentException("该车辆存在在场记录，不能删除");
        }
        vehicles.removeIf(item -> item.get("id").equals(currentVehicleId));
        appendLog("删除车辆档案", "删除车辆 " + vehicle.get("plateNumber"));
    }

    public synchronized List<Map<String, Object>> listParkingLots() {
        refreshParkingLotMetrics();
        return parkingLots;
    }

    public synchronized Map<String, Object> createParkingLot(ParkingLotCreateRequest request) {
        Map<String, Object> lot = new LinkedHashMap<>();
        long lotId = parkingLotId.incrementAndGet();
        lot.put("id", lotId);
        lot.put("name", request.name());
        lot.put("code", request.code());
        lot.put("address", request.address());
        lot.put("managerName", "系统新增");
        lot.put("businessHours", request.businessHours());
        lot.put("totalSpaces", request.totalSpaces());
        lot.put("occupiedSpaces", 0);
        lot.put("freeSpaces", request.totalSpaces());
        lot.put("status", "运营中");
        parkingLots.add(lot);

        for (int i = 1; i <= request.totalSpaces(); i++) {
            spaces.add(createSpace(lotId, request.code() + "-A-" + String.format("%03d", i), i <= 4 ? "新能源" : "普通"));
        }

        pricingRules.add(Map.of(
                "id", lotId,
                "parkingLotId", lotId,
                "parkingLotName", request.name(),
                "baseMinutes", 30,
                "baseFee", new BigDecimal("0"),
                "hourlyFee", new BigDecimal("4"),
                "dailyCap", new BigDecimal("38")
        ));
        appendLog("新增停车场", "新增停车场 " + request.name() + "，车位 " + request.totalSpaces() + " 个");
        refreshParkingLotMetrics();
        return lot;
    }

    public synchronized List<Map<String, Object>> listSpaces() {
        return spaces.stream()
                .sorted(Comparator.comparing(item -> String.valueOf(item.get("code"))))
                .toList();
    }

    public synchronized List<Map<String, Object>> listRegions() {
        return parkingLots.stream()
                .map(lot -> Map.of(
                        "parkingLotId", lot.get("id"),
                        "parkingLotName", lot.get("name"),
                        "regions", List.of(
                                Map.of("name", "A区", "type", "普通车位区", "available", countSpacesByRegion((Long) lot.get("id"), "A区", "空闲")),
                                Map.of("name", "B区", "type", "新能源车位区", "available", countSpacesByRegion((Long) lot.get("id"), "B区", "空闲"))
                        )
                ))
                .toList();
    }

    public synchronized List<Map<String, Object>> listPricingRules() {
        return pricingRules;
    }

    public synchronized List<Map<String, Object>> listRecords() {
        return parkingRecords.stream()
                .sorted(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("entryTime"))).reversed())
                .toList();
    }

    public synchronized List<Map<String, Object>> listOrders() {
        return orders.stream()
                .sorted(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("createdAt"))).reversed())
                .toList();
    }

    public synchronized Map<String, Object> checkIn(CheckInRequest request) {
        Map<String, Object> vehicle = getById(vehicles, request.vehicleId(), "车辆不存在");
        boolean alreadyInside = parkingRecords.stream()
                .anyMatch(item -> item.get("vehicleId").equals(request.vehicleId()) && "在场".equals(item.get("status")));
        if (alreadyInside) {
            throw new IllegalArgumentException("该车辆已在场内，不能重复入场");
        }

        Map<String, Object> lot = getById(parkingLots, request.parkingLotId(), "停车场不存在");
        Map<String, Object> freeSpace = spaces.stream()
                .filter(item -> item.get("parkingLotId").equals(request.parkingLotId()) && "空闲".equals(item.get("status")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("当前停车场暂无空闲车位"));

        freeSpace.put("status", "占用");
        LocalDateTime entryTime = LocalDateTime.now().minusMinutes(15);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", recordId.incrementAndGet());
        record.put("vehicleId", request.vehicleId());
        record.put("plateNumber", vehicle.get("plateNumber"));
        record.put("ownerName", vehicle.get("ownerName"));
        record.put("parkingLotId", request.parkingLotId());
        record.put("parkingLotName", lot.get("name"));
        record.put("spaceCode", freeSpace.get("code"));
        record.put("entryTime", FORMATTER.format(entryTime));
        record.put("exitTime", null);
        record.put("durationMinutes", 0);
        record.put("amount", BigDecimal.ZERO);
        record.put("status", "在场");
        parkingRecords.add(record);
        appendLog("车辆入场", vehicle.get("plateNumber") + " 进入 " + lot.get("name") + "，车位 " + freeSpace.get("code"));
        refreshParkingLotMetrics();
        return record;
    }

    public synchronized Map<String, Object> checkOut(Long currentRecordId) {
        Map<String, Object> record = getById(parkingRecords, currentRecordId, "停车记录不存在");
        if (!"在场".equals(record.get("status"))) {
            throw new IllegalArgumentException("该停车记录已出场");
        }
        LocalDateTime entryTime = LocalDateTime.parse(String.valueOf(record.get("entryTime")), FORMATTER);
        LocalDateTime exitTime = LocalDateTime.now();
        long minutes = Math.max(30, Duration.between(entryTime, exitTime).toMinutes());
        Map<String, Object> rule = pricingRules.stream()
                .filter(item -> item.get("parkingLotId").equals(record.get("parkingLotId")))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("未找到收费规则"));

        BigDecimal baseFee = (BigDecimal) rule.get("baseFee");
        BigDecimal hourlyFee = (BigDecimal) rule.get("hourlyFee");
        BigDecimal dailyCap = (BigDecimal) rule.get("dailyCap");
        BigDecimal hours = BigDecimal.valueOf(Math.ceil(minutes / 60.0));
        BigDecimal amount = baseFee.add(hourlyFee.multiply(hours)).min(dailyCap).setScale(2, RoundingMode.HALF_UP);

        record.put("exitTime", FORMATTER.format(exitTime));
        record.put("durationMinutes", minutes);
        record.put("amount", amount);
        record.put("status", "已完成");

        spaces.stream()
                .filter(item -> item.get("code").equals(record.get("spaceCode")))
                .findFirst()
                .ifPresent(item -> item.put("status", "空闲"));

        Map<String, Object> order = new LinkedHashMap<>();
        order.put("id", orderId.incrementAndGet());
        order.put("recordId", record.get("id"));
        order.put("plateNumber", record.get("plateNumber"));
        order.put("parkingLotName", record.get("parkingLotName"));
        order.put("amount", amount);
        order.put("paymentStatus", "已支付");
        order.put("createdAt", record.get("exitTime"));
        orders.add(order);

        appendLog("车辆出场", record.get("plateNumber") + " 离开 " + record.get("parkingLotName") + "，费用 " + amount + " 元");
        refreshParkingLotMetrics();
        return Map.of("record", record, "order", order);
    }

    public synchronized Map<String, Object> dashboardOverview() {
        refreshParkingLotMetrics();
        int totalSpaces = spaces.size();
        int freeSpaces = (int) spaces.stream().filter(item -> "空闲".equals(item.get("status"))).count();
        int occupiedSpaces = totalSpaces - freeSpaces;
        BigDecimal totalRevenue = orders.stream()
                .map(item -> (BigDecimal) item.get("amount"))
                .reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2, RoundingMode.HALF_UP);

        return Map.of(
                "metrics", List.of(
                        metric("停车场总数", parkingLots.size(), "管理全市核心停车资源"),
                        metric("车位总数", totalSpaces, "覆盖普通与新能源车位"),
                        metric("当前空闲车位", freeSpaces, "支持资源调度"),
                        metric("累计收入", totalRevenue + " 元", "基于已完成订单统计")
                ),
                "occupancyRate", percentage(occupiedSpaces, totalSpaces),
                "parkingTrend", parkingTrend(),
                "topParkingLots", parkingLots.stream()
                        .map(item -> Map.of(
                                "name", item.get("name"),
                                "occupancyRate", percentage((Integer) item.get("occupiedSpaces"), (Integer) item.get("totalSpaces")),
                                "freeSpaces", item.get("freeSpaces")
                        ))
                        .toList()
        );
    }

    public synchronized List<Map<String, Object>> dispatchSuggestions() {
        refreshParkingLotMetrics();
        return parkingLots.stream()
                .map(item -> {
                    int total = (Integer) item.get("totalSpaces");
                    int occupied = (Integer) item.get("occupiedSpaces");
                    double rate = total == 0 ? 0 : occupied * 100.0 / total;
                    String level = rate >= 85 ? "高" : rate >= 65 ? "中" : "低";
                    String action = rate >= 85
                            ? "建议引导车辆分流至周边停车场，并增加现场调度人员。"
                            : rate >= 65
                            ? "建议开启动态引导屏，优先分配新能源和临停车位。"
                            : "当前资源充足，可用于分担周边高峰压力。";
                    return Map.of(
                            "parkingLotName", item.get("name"),
                            "occupancyRate", percentage(occupied, total),
                            "warningLevel", level,
                            "action", action
                    );
                })
                .toList();
    }

    public synchronized List<Map<String, Object>> listNotices() {
        return notices;
    }

    public synchronized List<Map<String, Object>> listLogs() {
        return operationLogs.stream()
                .sorted(Comparator.comparing((Map<String, Object> item) -> String.valueOf(item.get("createdAt"))).reversed())
                .toList();
    }

    private void seedUsers() {
        users.add(user(1L, "admin", "系统管理员", "管理员", "负责系统配置、资源管理和全局监控", "13800000001"));
        users.add(user(2L, "manager", "停车场主管", "停车场管理员", "负责停车场资源维护与订单处理", "13800000002"));
        users.add(user(3L, "owner", "张晓彤", "车主", "负责车辆维护、停车查询和订单查看", "13800000003"));
        credentials.put("admin", "123456");
        credentials.put("manager", "123456");
        credentials.put("owner", "123456");
    }

    private void seedVehicles() {
        vehicles.add(vehicle(1L, 3L, "张晓彤", "沪A12345", "比亚迪海豹", "白色"));
        vehicles.add(vehicle(2L, 3L, "张晓彤", "沪B66218", "特斯拉Model 3", "灰色"));
        vehicles.add(vehicle(3L, 2L, "停车场主管", "沪C90871", "大众帕萨特", "黑色"));
        vehicles.add(vehicle(4L, 1L, "系统管理员", "沪D33688", "丰田凯美瑞", "银色"));
    }

    private void seedParking() {
        parkingLots.add(parkingLot(1L, "人民广场地下停车场", "PD-001", "上海市黄浦区人民大道 88 号", "徐晨", "06:00-23:30", 24));
        parkingLots.add(parkingLot(2L, "滨江商务停车中心", "PD-002", "上海市浦东新区滨江大道 166 号", "高宇", "全天开放", 20));

        for (int i = 1; i <= 24; i++) {
            spaces.add(createSpace(1L, "PD-001-" + (i <= 12 ? "A" : "B") + "-" + String.format("%03d", i), i <= 4 ? "新能源" : "普通"));
        }
        for (int i = 1; i <= 20; i++) {
            spaces.add(createSpace(2L, "PD-002-" + (i <= 10 ? "A" : "B") + "-" + String.format("%03d", i), i <= 3 ? "新能源" : "普通"));
        }

        pricingRules.add(Map.of(
                "id", 1L,
                "parkingLotId", 1L,
                "parkingLotName", "人民广场地下停车场",
                "baseMinutes", 30,
                "baseFee", new BigDecimal("0"),
                "hourlyFee", new BigDecimal("5"),
                "dailyCap", new BigDecimal("45")
        ));
        pricingRules.add(Map.of(
                "id", 2L,
                "parkingLotId", 2L,
                "parkingLotName", "滨江商务停车中心",
                "baseMinutes", 30,
                "baseFee", new BigDecimal("0"),
                "hourlyFee", new BigDecimal("4"),
                "dailyCap", new BigDecimal("40")
        ));
    }

    private void seedBusinessData() {
        occupySpace("PD-001-A-001");
        occupySpace("PD-001-A-002");
        occupySpace("PD-001-A-003");
        occupySpace("PD-002-A-001");
        occupySpace("PD-002-A-002");

        parkingRecords.add(record(1L, 1L, "沪A12345", "张晓彤", 1L, "人民广场地下停车场", "PD-001-A-001",
                LocalDateTime.now().minusHours(4), LocalDateTime.now().minusHours(2), 120, new BigDecimal("10"), "已完成"));
        parkingRecords.add(record(2L, 2L, "沪B66218", "张晓彤", 2L, "滨江商务停车中心", "PD-002-A-001",
                LocalDateTime.now().minusMinutes(90), null, 0, BigDecimal.ZERO, "在场"));

        orders.add(new LinkedHashMap<>(Map.of(
                "id", 1L,
                "recordId", 1L,
                "plateNumber", "沪A12345",
                "parkingLotName", "人民广场地下停车场",
                "amount", new BigDecimal("10"),
                "paymentStatus", "已支付",
                "createdAt", FORMATTER.format(LocalDateTime.now().minusHours(2))
        )));
        refreshParkingLotMetrics();
    }

    private void seedSystemData() {
        notices.add(Map.of(
                "id", 1L,
                "title", "毕业设计演示账号说明",
                "content", "车主登录账号：owner / 123456，管理员通过首页“进入后台管理”按钮进入。",
                "level", "重要"
        ));
        notices.add(Map.of(
                "id", 2L,
                "title", "系统演示范围",
                "content", "支持停车资源管理、车辆进出场、订单结算、数据看板、调度建议等核心流程。",
                "level", "普通"
        ));

        appendLog("初始化系统", "已完成示例数据装载，适合毕业设计答辩演示。");
        appendLog("生成团队包装", "项目已按 3 人团队分工完成材料口径整理。");
        appendLog("完成模块联调", "管理端、用户端和核心业务流程已对齐。");
    }

    private Map<String, Object> user(Long id, String username, String realName, String role, String responsibility, String phone) {
        return new LinkedHashMap<>(Map.of(
                "id", id,
                "username", username,
                "realName", realName,
                "role", role,
                "responsibility", responsibility,
                "phone", phone,
                "status", "启用"
        ));
    }

    private Map<String, Object> vehicle(Long id, Long ownerId, String ownerName, String plateNumber, String brand, String color) {
        return new LinkedHashMap<>(Map.of(
                "id", id,
                "userId", ownerId,
                "ownerName", ownerName,
                "plateNumber", plateNumber,
                "brand", brand,
                "color", color,
                "status", "正常"
        ));
    }

    private Map<String, Object> parkingLot(Long id, String name, String code, String address, String managerName, String businessHours, int totalSpaces) {
        return new LinkedHashMap<>(Map.of(
                "id", id,
                "name", name,
                "code", code,
                "address", address,
                "managerName", managerName,
                "businessHours", businessHours,
                "totalSpaces", totalSpaces,
                "occupiedSpaces", 0,
                "freeSpaces", totalSpaces,
                "status", "运营中"
        ));
    }

    private Map<String, Object> createSpace(Long lotId, String code, String category) {
        String region = code.contains("-A-") ? "A区" : "B区";
        return new LinkedHashMap<>(Map.of(
                "id", Long.valueOf(spaces.size() + 1L),
                "parkingLotId", lotId,
                "code", code,
                "regionName", region,
                "category", category,
                "status", "空闲"
        ));
    }

    private Map<String, Object> record(Long id, Long currentVehicleId, String plateNumber, String ownerName, Long currentLotId,
                                       String lotName, String spaceCode, LocalDateTime entryTime, LocalDateTime exitTime,
                                       long durationMinutes, BigDecimal amount, String status) {
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", id);
        record.put("vehicleId", currentVehicleId);
        record.put("plateNumber", plateNumber);
        record.put("ownerName", ownerName);
        record.put("parkingLotId", currentLotId);
        record.put("parkingLotName", lotName);
        record.put("spaceCode", spaceCode);
        record.put("entryTime", FORMATTER.format(entryTime));
        record.put("exitTime", exitTime == null ? null : FORMATTER.format(exitTime));
        record.put("durationMinutes", durationMinutes);
        record.put("amount", amount);
        record.put("status", status);
        return record;
    }

    private Map<String, Object> metric(String title, Object value, String description) {
        return Map.of("title", title, "value", value, "description", description);
    }

    private void occupySpace(String code) {
        spaces.stream()
                .filter(item -> item.get("code").equals(code))
                .findFirst()
                .ifPresent(item -> item.put("status", "占用"));
    }

    private int countSpacesByRegion(Long lotId, String regionName, String status) {
        return (int) spaces.stream()
                .filter(item -> item.get("parkingLotId").equals(lotId))
                .filter(item -> item.get("regionName").equals(regionName))
                .filter(item -> item.get("status").equals(status))
                .count();
    }

    private void refreshParkingLotMetrics() {
        for (Map<String, Object> lot : parkingLots) {
            long currentLotId = (Long) lot.get("id");
            int total = (int) spaces.stream().filter(item -> item.get("parkingLotId").equals(currentLotId)).count();
            int occupied = (int) spaces.stream()
                    .filter(item -> item.get("parkingLotId").equals(currentLotId))
                    .filter(item -> "占用".equals(item.get("status")))
                    .count();
            lot.put("totalSpaces", total);
            lot.put("occupiedSpaces", occupied);
            lot.put("freeSpaces", total - occupied);
        }
    }

    private String percentage(int numerator, int denominator) {
        if (denominator == 0) {
            return "0%";
        }
        return BigDecimal.valueOf(numerator * 100.0 / denominator)
                .setScale(1, RoundingMode.HALF_UP) + "%";
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
        return Map.of(
                "time", time,
                "entries", entries,
                "exits", exits
        );
    }

    private void appendLog(String action, String detail) {
        operationLogs.add(new LinkedHashMap<>(Map.of(
                "id", logId.incrementAndGet(),
                "action", action,
                "detail", detail,
                "createdAt", FORMATTER.format(LocalDateTime.now())
        )));
    }

    private Map<String, Object> getById(List<Map<String, Object>> items, Long id, String message) {
        return items.stream()
                .filter(item -> item.get("id").equals(id))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException(message));
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
