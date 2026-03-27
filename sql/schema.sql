SET NAMES utf8mb4;

CREATE DATABASE IF NOT EXISTS city_parking_resource_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE city_parking_resource_management;

DROP TABLE IF EXISTS sys_operation_log;
DROP TABLE IF EXISTS sys_notice;
DROP TABLE IF EXISTS biz_order;
DROP TABLE IF EXISTS biz_parking_record;
DROP TABLE IF EXISTS biz_pricing_rule;
DROP TABLE IF EXISTS biz_parking_space;
DROP TABLE IF EXISTS biz_parking_region;
DROP TABLE IF EXISTS biz_parking_lot;
DROP TABLE IF EXISTS biz_vehicle;
DROP TABLE IF EXISTS sys_user_role;
DROP TABLE IF EXISTS sys_role;
DROP TABLE IF EXISTS sys_user;

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(100) NOT NULL,
    real_name VARCHAR(50) NOT NULL,
    phone VARCHAR(20),
    role_name VARCHAR(30) NOT NULL,
    status VARCHAR(20) DEFAULT '启用',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    role_code VARCHAR(50) NOT NULL UNIQUE,
    role_name VARCHAR(50) NOT NULL,
    description VARCHAR(255)
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES sys_user(id),
    CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES sys_role(id)
);

CREATE TABLE biz_vehicle (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    plate_number VARCHAR(20) NOT NULL UNIQUE,
    brand VARCHAR(50),
    color VARCHAR(20),
    status VARCHAR(20) DEFAULT '正常',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_vehicle_user FOREIGN KEY (user_id) REFERENCES sys_user(id)
);

CREATE TABLE biz_parking_lot (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) NOT NULL UNIQUE,
    address VARCHAR(200) NOT NULL,
    manager_name VARCHAR(50),
    business_hours VARCHAR(50),
    total_spaces INT NOT NULL,
    occupied_spaces INT DEFAULT 0,
    free_spaces INT DEFAULT 0,
    status VARCHAR(20) DEFAULT '运营中'
);

CREATE TABLE biz_parking_region (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parking_lot_id BIGINT NOT NULL,
    region_name VARCHAR(50) NOT NULL,
    region_type VARCHAR(50) NOT NULL,
    CONSTRAINT fk_region_lot FOREIGN KEY (parking_lot_id) REFERENCES biz_parking_lot(id)
);

CREATE TABLE biz_parking_space (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parking_lot_id BIGINT NOT NULL,
    region_id BIGINT,
    code VARCHAR(50) NOT NULL UNIQUE,
    category VARCHAR(30) NOT NULL,
    status VARCHAR(20) DEFAULT '空闲',
    CONSTRAINT fk_space_lot FOREIGN KEY (parking_lot_id) REFERENCES biz_parking_lot(id),
    CONSTRAINT fk_space_region FOREIGN KEY (region_id) REFERENCES biz_parking_region(id)
);

CREATE TABLE biz_pricing_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    parking_lot_id BIGINT NOT NULL,
    base_minutes INT DEFAULT 30,
    base_fee DECIMAL(10, 2) DEFAULT 0.00,
    hourly_fee DECIMAL(10, 2) NOT NULL,
    daily_cap DECIMAL(10, 2) NOT NULL,
    CONSTRAINT fk_pricing_lot FOREIGN KEY (parking_lot_id) REFERENCES biz_parking_lot(id)
);

CREATE TABLE biz_parking_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    vehicle_id BIGINT NOT NULL,
    parking_lot_id BIGINT NOT NULL,
    parking_space_id BIGINT,
    entry_time DATETIME NOT NULL,
    exit_time DATETIME,
    duration_minutes INT DEFAULT 0,
    amount DECIMAL(10, 2) DEFAULT 0.00,
    status VARCHAR(20) DEFAULT '在场',
    CONSTRAINT fk_record_vehicle FOREIGN KEY (vehicle_id) REFERENCES biz_vehicle(id),
    CONSTRAINT fk_record_lot FOREIGN KEY (parking_lot_id) REFERENCES biz_parking_lot(id),
    CONSTRAINT fk_record_space FOREIGN KEY (parking_space_id) REFERENCES biz_parking_space(id)
);

CREATE TABLE biz_order (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    record_id BIGINT NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    payment_status VARCHAR(20) DEFAULT '已支付',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_order_record FOREIGN KEY (record_id) REFERENCES biz_parking_record(id)
);

CREATE TABLE sys_notice (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(100) NOT NULL,
    content TEXT NOT NULL,
    level_name VARCHAR(20) DEFAULT '普通',
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE sys_operation_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    action_name VARCHAR(50) NOT NULL,
    detail_text VARCHAR(255) NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_vehicle_user ON biz_vehicle(user_id);
CREATE INDEX idx_space_lot ON biz_parking_space(parking_lot_id);
CREATE INDEX idx_record_vehicle ON biz_parking_record(vehicle_id);
CREATE INDEX idx_record_lot ON biz_parking_record(parking_lot_id);
CREATE INDEX idx_order_record ON biz_order(record_id);
