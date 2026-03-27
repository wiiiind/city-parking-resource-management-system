# 城市停车资源管理系统

基于 `Spring Boot 3 + Vue 3 + MySQL 8` 的毕业设计项目，主题为城市停车资源统一管理。系统围绕停车场资源维护、车辆进出场、费用结算、数据看板、资源调度建议和毕业材料整理展开，适合作为本科毕设的代码成果与答辩演示系统。

## 项目简介

本项目面向 2 类角色：

- 系统管理员：负责经营数据查看、订单记录查看和停车场看板分析
- 车主：负责账号注册、停车查询、车辆档案维护、停车缴费和订单查询

系统实现了以下核心能力：

- 用户注册、登录与角色入口
- 停车场、区域、车位、收费规则管理
- 车辆入场、出场、费用结算、订单生成、车辆删除
- 停车资源统计、停车流量趋势、调度建议
- 公告信息、操作日志、团队分工展示

## 技术栈

- 后端：Spring Boot 3.5、Java 17、REST API
- 前端：Vue 3、TypeScript、Vite
- 数据库：MySQL 8
- 文档：Markdown、Mermaid

## 目录结构

```text
city-parking-resource-management-system/
├── backend/        Spring Boot 后端
├── frontend/       Vue 3 前端
├── sql/            数据库建表与初始化脚本
├── docs/           项目计划书、技术文档、测试用例、部署说明、报告册
├── scripts/        辅助脚本
├── deliverables/   交付材料输出目录
└── README.md
```

## 环境要求

- Node.js 20+
- npm 10+
- JDK 17
- MySQL 8

当前 WSL 已安装本地 JDK：

```bash
java -version
```

如果输出 `openjdk 17`，说明 Java 环境已经就绪。

## 快速启动

### 1. 启动后端

```bash
cd backend
./mvnw spring-boot:run
```

启动成功后默认地址：

- 应用端口：`http://localhost:8080`
- API 前缀：`http://localhost:8080/api`

接口自检示例：

```bash
curl http://localhost:8080/api/dashboard/overview
```

### 2. 启动前端

```bash
cd frontend
npm install
npm run dev -- --host 0.0.0.0
```

前端访问地址：

- 本机访问：`http://localhost:5173`

### 3. 前端构建

```bash
cd frontend
npm run build
```

### 4. 后端测试

```bash
cd backend
./mvnw test
```

## 演示入口

- 车主登录：`owner / 123456`
- 管理员入口：首页点击“进入后台管理”
- 新车主可通过首页注册功能创建账号

## 数据库脚本

数据库脚本位于：

- `sql/bootstrap.sql`
- `sql/schema.sql`
- `sql/data.sql`

推荐初始化方式：

```bash
sudo mysql < sql/bootstrap.sql
```

`bootstrap.sql` 会完成以下工作：

1. 创建数据库 `city_parking_resource_management`
2. 创建应用账号 `city_parking_app`
3. 授权应用账号访问数据库
4. 执行 `schema.sql` 建表
5. 执行 `data.sql` 导入演示数据

说明：

- 当前后端已接入真实 MySQL 数据库运行
- SQL 可一键初始化同款数据库环境，便于答辩演示和项目复现

## 当前已验证

- 前端 `npm run build` 通过
- 后端 `./mvnw test` 通过
- 前后端本地联调通过
- 已实际验证以下主流程：
  - 新增停车场
  - 新增车辆
  - 删除车辆
  - 车辆入场
  - 车辆出场结算
  - 看板与日志同步刷新

## 推荐答辩演示顺序

1. 打开首页，展示项目主题、团队分工、综合看板
2. 使用车主账号登录，演示停车查询、车辆管理、停车记录和订单查看
3. 选择停车场执行车辆入场
4. 在停车记录页完成出场结算
5. 退出后从管理员入口进入后台
6. 展示收入统计、停车记录订单记录和单停车场看板
7. 展示日志、占用率变化和调度建议
8. 最后打开文档目录，展示技术文档、测试用例、报告册和学习日志

## 交付材料

仓库已包含毕设常用材料：

- `docs/项目计划书.md`
- `docs/技术文档.md`
- `docs/测试用例.md`
- `docs/部署说明.md`
- `docs/毕业实习报告册.md`
- `deliverables/学习日志.xlsx`

## 说明

本项目当前版本定位为“适合毕业设计演示与交付”的完整作品版本。后续如果需要，我还可以继续帮你补：

- 更正式的登录鉴权
- 真正的 MySQL 持久化接入
- 系统截图整理
- 答辩讲解词
- 论文中的系统实现章节内容
