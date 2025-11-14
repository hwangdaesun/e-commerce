-- 데이터베이스 생성
CREATE DATABASE IF NOT EXISTS ecommerce CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

-- 사용자 생성 및 권한 부여
CREATE USER IF NOT EXISTS 'ecommerce_user'@'%' IDENTIFIED BY 'ecommerce_pass';
GRANT ALL PRIVILEGES ON ecommerce.* TO 'ecommerce_user'@'%';
FLUSH PRIVILEGES;

-- 데이터베이스 선택
USE ecommerce;