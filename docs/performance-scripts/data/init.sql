-- ============================================
-- K6 부하 테스트용 초기 데이터 생성 스크립트
-- ============================================
--
-- 목적: 부하 테스트에 필요한 대량의 기본 데이터 생성 (성공 케이스 중심)
-- 생성 데이터:
--   - 사용자: 100만명 (user_id: 1~1,000,000, 포인트: 10,000,000)
--   - 상품: 100만개 (item_id: 1~1,000,000, 재고: 10,000)
--   - 쿠폰: 50만개 (coupon_id: 1~500,000, 각 재고: 100,000)
--   - 사용자 쿠폰: 400만개 (user 1~200,000에게 각 20개씩)
--   - 장바구니: 20만개 (cart_id: 1~200,000)
--   - 장바구니 아이템: 60만개 (cart_item_id: 1~600,000)
--
-- 실행 시간: 약 5~10분 (데이터베이스 성능에 따라 다름)
--
-- 실행 방법:
--   mysql -u root -p ecommerce < init.sql
--
-- 주의사항:
--   1. 충분한 디스크 공간 확보 (최소 10GB 권장)
--   2. innodb_buffer_pool_size 충분히 설정
--   3. 실행 전 기존 데이터 백업 권장
-- ============================================

USE ecommerce;

-- 성능 최적화 설정 (임시)
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- ============================================
-- 0. 기존 데이터 초기화
-- ============================================

SELECT '========================================' AS '';
SELECT '⚠️  기존 데이터를 삭제합니다...' AS '';
SELECT '========================================' AS '';

-- 주문 관련 데이터 삭제
SELECT '주문 데이터 삭제 중...' AS status;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;

-- 장바구니 데이터 삭제
SELECT '장바구니 데이터 삭제 중...' AS status;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE carts;

-- 쿠폰 데이터 삭제
SELECT '쿠폰 데이터 삭제 중...' AS status;
TRUNCATE TABLE user_coupons;
TRUNCATE TABLE coupon_stocks;
TRUNCATE TABLE coupons;

-- 상품 데이터 삭제
SELECT '상품 데이터 삭제 중...' AS status;
TRUNCATE TABLE items;

-- 포인트 데이터 삭제
SELECT '포인트 데이터 삭제 중...' AS status;
TRUNCATE TABLE user_points;

-- 사용자 데이터 삭제
SELECT '사용자 데이터 삭제 중...' AS status;
TRUNCATE TABLE users;

SELECT '✅ 기존 데이터 삭제 완료' AS '';
SELECT '========================================' AS '';
SELECT '' AS '';

-- ============================================
-- 1. 사용자 데이터 생성 (100만명)
-- ============================================

SELECT '1/5: 사용자 데이터 생성 중... (1,000,000명)' AS status;

DROP PROCEDURE IF EXISTS generate_users;

DELIMITER //
CREATE PROCEDURE generate_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 1000000;

    WHILE i <= total DO
        -- 배치 단위로 INSERT
        INSERT INTO users (user_id, created_at, updated_at)
        SELECT
            i + n - 1 AS user_id,
            NOW() AS created_at,
            NOW() AS updated_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        -- user_points도 함께 생성 (초기 포인트 1000만 - 성공 케이스 보장)
        INSERT INTO user_points (user_id, point, updated_at)
        SELECT
            i + n - 1 AS user_id,
            10000000 AS point,
            NOW() AS updated_at
        FROM (
            SELECT @row2 := @row2 + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row2 := 0) r
            WHERE @row2 < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 100000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_users();
DROP PROCEDURE generate_users;

SELECT CONCAT('✅ 사용자 생성 완료: ', COUNT(*), '명') AS result FROM users;
SELECT CONCAT('✅ 사용자 포인트 생성 완료: ', COUNT(*), '건') AS result FROM user_points;

-- ============================================
-- 2. 상품 데이터 생성 (100만개)
-- ============================================

SELECT '2/5: 상품 데이터 생성 중... (1,000,000개)' AS status;

DROP PROCEDURE IF EXISTS generate_items;

DELIMITER //
CREATE PROCEDURE generate_items()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 1000000;

    WHILE i <= total DO
        INSERT INTO items (item_id, name, price, stock, version, created_at, updated_at)
        SELECT
            i + n - 1 AS item_id,
            CONCAT('상품_', i + n - 1) AS name,
            (1000 + (i + n - 1) % 99000) AS price,  -- 1,000원 ~ 100,000원
            10000 AS stock,  -- 기본 재고 10,000개 (성공 케이스 보장)
            0 AS version,
            NOW() AS created_at,
            NOW() AS updated_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 100000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_items();
DROP PROCEDURE generate_items;

SELECT CONCAT('✅ 상품 생성 완료: ', COUNT(*), '개') AS result FROM items;

-- ============================================
-- 3. 쿠폰 데이터 생성 (50만개)
-- ============================================

SELECT '3/6: 쿠폰 데이터 생성 중... (500,000개)' AS status;

DROP PROCEDURE IF EXISTS generate_coupons;

DELIMITER //
CREATE PROCEDURE generate_coupons()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 500000;

    WHILE i <= total DO
        -- 쿠폰 생성
        INSERT INTO coupons (coupon_id, name, discount_amount, total_quantity, expires_at, created_at, updated_at)
        SELECT
            i + n - 1 AS coupon_id,
            CONCAT('쿠폰_', i + n - 1) AS name,
            (1000 + ((i + n - 1) % 9) * 1000) AS discount_amount,  -- 1,000원 ~ 10,000원
            100000 AS total_quantity,
            DATE_ADD(NOW(), INTERVAL 365 DAY) AS expires_at,
            NOW() AS created_at,
            NOW() AS updated_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        -- 쿠폰 재고 생성
        INSERT INTO coupon_stocks (coupon_id, remaining_quantity, updated_at)
        SELECT
            i + n - 1 AS coupon_id,
            100000 AS remaining_quantity,
            NOW() AS updated_at
        FROM (
            SELECT @row2 := @row2 + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT @row2 := 0) r
            WHERE @row2 < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 100000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_coupons();
DROP PROCEDURE generate_coupons;

SELECT CONCAT('✅ 쿠폰 생성 완료: ', COUNT(*), '개') AS result FROM coupons;
SELECT CONCAT('✅ 쿠폰 재고 생성 완료: ', COUNT(*), '건') AS result FROM coupon_stocks;

-- ============================================
-- 4. 사용자 쿠폰 발급 (User 1~200,000에게 각 20개씩)
-- ============================================

SELECT '4/6: 사용자 쿠폰 발급 중... (4,000,000건)' AS status;

DROP PROCEDURE IF EXISTS generate_user_coupons;

DELIMITER //
CREATE PROCEDURE generate_user_coupons()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 4000000;  -- 200,000 users * 20 coupons

    WHILE i <= total DO
        INSERT INTO user_coupons (user_id, coupon_id, is_used, issued_at)
        SELECT
            ((i + n - 2) DIV 20) + 1 AS user_id,  -- User 1~200,000
            ((i + n - 1) % 500000) + 1 AS coupon_id,  -- 쿠폰 순환 사용
            0 AS is_used,
            NOW() AS issued_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 500000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_user_coupons();
DROP PROCEDURE generate_user_coupons;

SELECT CONCAT('✅ 사용자 쿠폰 발급 완료: ', COUNT(*), '건') AS result FROM user_coupons;
SELECT CONCAT('  - User 1~200,000: 각 ',
    ROUND(COUNT(*) / 200000, 0), '개씩 쿠폰 보유') AS detail
FROM user_coupons;

-- ============================================
-- 5. 장바구니 데이터 생성 (20만개)
-- ============================================

SELECT '5/6: 장바구니 데이터 생성 중... (200,000개)' AS status;

DROP PROCEDURE IF EXISTS generate_carts;

DELIMITER //
CREATE PROCEDURE generate_carts()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 200000;

    WHILE i <= total DO
        INSERT INTO carts (cart_id, user_id, created_at, updated_at)
        SELECT
            i + n - 1 AS cart_id,
            (i + n - 1) AS user_id,  -- user 1~200,000이 각각 장바구니 1개씩 소유
            NOW() AS created_at,
            NOW() AS updated_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 50000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_carts();
DROP PROCEDURE generate_carts;

SELECT CONCAT('✅ 장바구니 생성 완료: ', COUNT(*), '개') AS result FROM carts;

-- ============================================
-- 6. 장바구니 아이템 데이터 생성 (60만개)
-- ============================================

SELECT '6/6: 장바구니 아이템 데이터 생성 중... (600,000개)' AS status;

DROP PROCEDURE IF EXISTS generate_cart_items;

DELIMITER //
CREATE PROCEDURE generate_cart_items()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total INT DEFAULT 600000;
    DECLARE cart_count INT DEFAULT 200000;

    WHILE i <= total DO
        INSERT INTO cart_items (cart_item_id, cart_id, item_id, quantity, created_at, updated_at)
        SELECT
            i + n - 1 AS cart_item_id,
            -- 각 장바구니에 약 3개씩 아이템 분배
            ((i + n - 2) DIV 3) + 1 AS cart_id,
            -- 랜덤하게 보이도록 item_id 분산 (실제로는 순차적이지만 mod 연산으로 분산)
            ((i + n - 1) * 7919 % 1000000) + 1 AS item_id,
            ((i + n - 1) % 3) + 1 AS quantity,  -- 1~3개
            NOW() AS created_at,
            NOW() AS updated_at
        FROM (
            SELECT @row := @row + 1 AS n
            FROM
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t1,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t2,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t3,
                (SELECT 0 UNION SELECT 1 UNION SELECT 2 UNION SELECT 3 UNION SELECT 4
                 UNION SELECT 5 UNION SELECT 6 UNION SELECT 7 UNION SELECT 8 UNION SELECT 9) t4,
                (SELECT @row := 0) r
            WHERE @row < batch_size
        ) numbers
        WHERE i + n - 1 <= total;

        COMMIT;
        SET i = i + batch_size;

        IF i % 100000 = 1 THEN
            SELECT CONCAT('  진행률: ', FLOOR((i / total) * 100), '%') AS progress;
        END IF;
    END WHILE;
END//
DELIMITER ;

CALL generate_cart_items();
DROP PROCEDURE generate_cart_items;

SELECT CONCAT('✅ 장바구니 아이템 생성 완료: ', COUNT(*), '개') AS result FROM cart_items;

-- 원래 설정으로 복원
SET foreign_key_checks = 1;
SET unique_checks = 1;
SET autocommit = 1;

-- ============================================
-- 데이터 생성 완료 요약
-- ============================================

SELECT '========================================' AS '';
SELECT '✅ 초기 데이터 생성 완료!' AS '';
SELECT '========================================' AS '';

SELECT '테이블별 생성 건수' AS category, '' AS count, '' AS detail
UNION ALL
SELECT '─────────────────', '──────────', '────────────────'
UNION ALL
SELECT 'users', CONCAT(FORMAT(COUNT(*), 0), '건'), '포인트: 10,000,000' FROM users
UNION ALL
SELECT 'user_points', CONCAT(FORMAT(COUNT(*), 0), '건'), '' FROM user_points
UNION ALL
SELECT 'items', CONCAT(FORMAT(COUNT(*), 0), '건'), '재고: 10,000개' FROM items
UNION ALL
SELECT 'coupons', CONCAT(FORMAT(COUNT(*), 0), '건'), '각 재고: 100,000' FROM coupons
UNION ALL
SELECT 'coupon_stocks', CONCAT(FORMAT(COUNT(*), 0), '건'), '' FROM coupon_stocks
UNION ALL
SELECT 'user_coupons', CONCAT(FORMAT(COUNT(*), 0), '건'), 'User 1~200K: 각 20개' FROM user_coupons
UNION ALL
SELECT 'carts', CONCAT(FORMAT(COUNT(*), 0), '건'), '' FROM carts
UNION ALL
SELECT 'cart_items', CONCAT(FORMAT(COUNT(*), 0), '건'), '' FROM cart_items;

SELECT '========================================' AS '';
SELECT '✅ 성공 케이스 중심 테스트 데이터 준비 완료!' AS '';
SELECT '' AS '';
SELECT '📌 주요 특징:' AS '';
SELECT '  - 모든 사용자: 포인트 10,000,000 (충분한 잔액)' AS '';
SELECT '  - 모든 상품: 재고 10,000개 (재고 부족 최소화)' AS '';
SELECT '  - 쿠폰: 500,000개 (각 100,000개 재고)' AS '';
SELECT '  - User 1~200,000: 각 20개 쿠폰 보유' AS '';
SELECT '' AS '';
SELECT '🚀 이제 K6 부하 테스트를 실행할 수 있습니다!' AS '';
SELECT '========================================' AS '';