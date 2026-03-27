CREATE DATABASE IF NOT EXISTS city_parking_resource_management DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'city_parking_app'@'localhost' IDENTIFIED BY '123456';
GRANT ALL PRIVILEGES ON city_parking_resource_management.* TO 'city_parking_app'@'localhost';
FLUSH PRIVILEGES;

SOURCE sql/schema.sql;
SOURCE sql/data.sql;
