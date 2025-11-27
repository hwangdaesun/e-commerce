-- 대용량 데이터 생성 스크립트 (사용자 100만, 주문 500만)
USE ecommerce;

-- 성능 최적화 설정
SET autocommit = 0;
SET unique_checks = 0;
SET foreign_key_checks = 0;

-- 1. 사용자 데이터 생성 (100만 건)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_users()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_count INT DEFAULT 1000000;

    SELECT COUNT(*) INTO @existing_count FROM user_points;
    IF @existing_count > 0 THEN
        SELECT 'Users already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 1,000,000 users with points...' AS message;

        WHILE i <= total_count DO
            -- 사용자 포인트 생성 (user_id가 PK)
            INSERT INTO user_points (user_id, point, updated_at)
            VALUES (
                i,
                FLOOR(RAND() * 500000),  -- 0 ~ 500,000 포인트
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY)
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 100000 = 0 THEN
                SELECT CONCAT(i, ' users generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '1,000,000 users generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 2. 상품 데이터 생성 (100만 건 - 대형 이커머스 규모)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_items()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_count INT DEFAULT 1000000;
    DECLARE category_names VARCHAR(500) DEFAULT 'Electronics,Fashion,Food,Books,Home,Sports,Beauty,Toys,Health,Automotive';
    DECLARE category_name VARCHAR(50);

    SELECT COUNT(*) INTO @existing_count FROM items;
    IF @existing_count > 0 THEN
        SELECT 'Items already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 1,000,000 items...' AS message;

        WHILE i <= total_count DO
            SET category_name = SUBSTRING_INDEX(SUBSTRING_INDEX(category_names, ',', FLOOR(1 + RAND() * 10)), ',', -1);

            INSERT INTO items (name, price, stock, created_at, updated_at)
            VALUES (
                CONCAT(category_name, '_', LPAD(i, 6, '0')),
                FLOOR(1000 + RAND() * 499000),   -- 1,000 ~ 500,000원
                FLOOR(10 + RAND() * 990),         -- 10 ~ 1,000개
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 730) DAY),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 100000 = 0 THEN
                SELECT CONCAT(i, ' items generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '1,000,000 items generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 3. 쿠폰 데이터 생성 (10,000개)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_coupons()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 1000;
    DECLARE total_count INT DEFAULT 10000;
    DECLARE coupon_types VARCHAR(200) DEFAULT '신규가입,첫구매,재구매,VIP,일반,시즌특가,생일축하,추천인,리뷰작성,앱전용';
    DECLARE coupon_type VARCHAR(50);
    DECLARE discount INT;
    DECLARE quantity INT;

    SELECT COUNT(*) INTO @existing_count FROM coupons;
    IF @existing_count > 0 THEN
        SELECT 'Coupons already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 10,000 coupons...' AS message;

        WHILE i <= total_count DO
            SET coupon_type = SUBSTRING_INDEX(SUBSTRING_INDEX(coupon_types, ',', FLOOR(1 + RAND() * 10)), ',', -1);
            SET discount = (FLOOR(RAND() * 20) + 1) * 1000;  -- 1,000 ~ 20,000원
            SET quantity = FLOOR(1000 + RAND() * 99000);      -- 1,000 ~ 100,000개

            INSERT INTO coupons (name, discount_amount, total_quantity, expires_at, created_at, updated_at)
            VALUES (
                CONCAT(coupon_type, '_쿠폰_', LPAD(i, 3, '0')),
                discount,
                quantity,
                DATE_ADD(NOW(), INTERVAL FLOOR(30 + RAND() * 335) DAY),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 180) DAY),
                NOW()
            );

            -- 쿠폰 재고 생성
            INSERT INTO coupon_stocks (coupon_id, remaining_quantity, updated_at)
            VALUES (
                i,
                FLOOR(quantity * 0.3 + RAND() * quantity * 0.5),  -- 30~80% 남음
                NOW()
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 2000 = 0 THEN
                SELECT CONCAT(i, ' coupons generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '10,000 coupons generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 4. 주문 데이터 생성 (500만 건, PAID 96%, PENDING 4%)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_orders()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE total_count INT DEFAULT 5000000;
    DECLARE user_id_val BIGINT;
    DECLARE total_amount_val INT;
    DECLARE coupon_discount_val INT;
    DECLARE order_status VARCHAR(10);
    DECLARE rand_val FLOAT;

    SELECT COUNT(*) INTO @existing_count FROM orders;
    IF @existing_count > 0 THEN
        SELECT 'Orders already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 5,000,000 orders (PAID 96%, PENDING 4%)...' AS message;

        WHILE i <= total_count DO
            SET user_id_val = FLOOR(1 + RAND() * 1000000);
            SET total_amount_val = FLOOR(5000 + RAND() * 495000);  -- 5,000 ~ 500,000원
            SET coupon_discount_val = IF(RAND() > 0.6, FLOOR(RAND() * 20000), 0);  -- 40% 쿠폰 사용

            -- OrderStatus: PAID 96%, PENDING 4%
            SET rand_val = RAND();
            IF rand_val < 0.96 THEN
                SET order_status = 'PAID';
            ELSE
                SET order_status = 'PENDING';
            END IF;

            INSERT INTO orders (user_id, status, total_amount, coupon_discount, final_amount, created_at, updated_at)
            VALUES (
                user_id_val,
                order_status,
                total_amount_val,
                coupon_discount_val,
                GREATEST(total_amount_val - coupon_discount_val, 0),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY)
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 500000 = 0 THEN
                SELECT CONCAT(i, ' orders generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '5,000,000 orders generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 5. 주문 아이템 데이터 생성 (약 1250만 건 - 주문당 평균 2.5개)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_order_items()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE j INT;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE items_per_order INT;
    DECLARE total_orders INT DEFAULT 5000000;
    DECLARE item_id_val BIGINT;
    DECLARE item_price INT;

    SELECT COUNT(*) INTO @existing_count FROM order_items;
    IF @existing_count > 0 THEN
        SELECT 'Order items already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating order items (avg 2.5 per order = ~12.5M items)...' AS message;

        WHILE i <= total_orders DO
            -- 주문당 1~5개 아이템 (평균 2.5개)
            SET items_per_order = FLOOR(1 + RAND() * 5);
            SET j = 1;

            WHILE j <= items_per_order DO
                SET item_id_val = FLOOR(1 + RAND() * 1000000);
                SET item_price = FLOOR(1000 + RAND() * 499000);

                INSERT INTO order_items (order_id, item_id, name, price, quantity, user_coupon_id)
                VALUES (
                    i,
                    item_id_val,
                    CONCAT('Item_', LPAD(item_id_val, 7, '0')),
                    item_price,
                    FLOOR(1 + RAND() * 5),  -- 1~5개 주문
                    IF(RAND() > 0.7, FLOOR(1 + RAND() * 10000), NULL)  -- 30% 쿠폰 사용
                );
                SET j = j + 1;
            END WHILE;

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 500000 = 0 THEN
                SELECT CONCAT(i, ' orders processed...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT 'Order items generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 6. 장바구니 데이터 생성 (20만 건 - 활성 사용자 20%)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_carts()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE total_count INT DEFAULT 200000;
    DECLARE user_id_val BIGINT;

    SELECT COUNT(*) INTO @existing_count FROM carts;
    IF @existing_count > 0 THEN
        SELECT 'Carts already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 200,000 carts...' AS message;

        WHILE i <= total_count DO
            SET user_id_val = FLOOR(1 + RAND() * 1000000);

            INSERT INTO carts (user_id, created_at, updated_at)
            VALUES (
                user_id_val,
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 90) DAY),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 50000 = 0 THEN
                SELECT CONCAT(i, ' carts generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '200,000 carts generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 7. 장바구니 아이템 데이터 생성 (60만 건 - 장바구니당 평균 3개)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_cart_items()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE j INT;
    DECLARE batch_size INT DEFAULT 5000;
    DECLARE items_per_cart INT;
    DECLARE total_carts INT DEFAULT 200000;

    SELECT COUNT(*) INTO @existing_count FROM cart_items;
    IF @existing_count > 0 THEN
        SELECT 'Cart items already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating cart items (avg 3 per cart = 600k items)...' AS message;

        WHILE i <= total_carts DO
            SET items_per_cart = FLOOR(1 + RAND() * 5);  -- 1~5개
            SET j = 1;

            WHILE j <= items_per_cart DO
                INSERT INTO cart_items (cart_id, item_id, quantity, created_at, updated_at)
                VALUES (
                    i,
                    FLOOR(1 + RAND() * 1000000),
                    FLOOR(1 + RAND() * 10),
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 30) DAY),
                    DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 7) DAY)
                );
                SET j = j + 1;
            END WHILE;

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 50000 = 0 THEN
                SELECT CONCAT(i, ' carts processed...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT 'Cart items generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 8. 사용자 쿠폰 데이터 생성 (500만 건 - 사용자당 평균 5개)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_user_coupons()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_count INT DEFAULT 5000000;
    DECLARE is_used_val BOOLEAN;
    DECLARE issued_date DATETIME;

    SELECT COUNT(*) INTO @existing_count FROM user_coupons;
    IF @existing_count > 0 THEN
        SELECT 'User coupons already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 5,000,000 user coupons...' AS message;

        WHILE i <= total_count DO
            SET is_used_val = RAND() > 0.5;  -- 50% 사용됨
            SET issued_date = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY);

            INSERT INTO user_coupons (user_id, coupon_id, is_used, used_at, issued_at)
            VALUES (
                FLOOR(1 + RAND() * 1000000),
                FLOOR(1 + RAND() * 10000),
                is_used_val,
                IF(is_used_val, DATE_ADD(issued_date, INTERVAL FLOOR(RAND() * 180) DAY), NULL),
                issued_date
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 500000 = 0 THEN
                SELECT CONCAT(i, ' user coupons generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '5,000,000 user coupons generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 9. 상품 조회 기록 생성 (5000만 건 - 사용자당 평균 50건)
DELIMITER //
CREATE PROCEDURE IF NOT EXISTS generate_item_views()
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE batch_size INT DEFAULT 10000;
    DECLARE total_count INT DEFAULT 50000000;

    SELECT COUNT(*) INTO @existing_count FROM item_views;
    IF @existing_count > 0 THEN
        SELECT 'Item views already exist, skipping...' AS message;
    ELSE
        SELECT 'Generating 50,000,000 item views...' AS message;

        WHILE i <= total_count DO
            INSERT INTO item_views (item_id, user_id, created_at, updated_at)
            VALUES (
                FLOOR(1 + RAND() * 1000000),
                FLOOR(1 + RAND() * 1000000),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY),
                DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * 365) DAY)
            );

            SET i = i + 1;

            IF i % batch_size = 0 THEN
                COMMIT;
            END IF;

            IF i % 5000000 = 0 THEN
                SELECT CONCAT(i, ' item views generated...') AS progress;
            END IF;
        END WHILE;

        COMMIT;
        SELECT '50,000,000 item views generated successfully!' AS message;
    END IF;
END //
DELIMITER ;

-- 기존 데이터 삭제 (재실행 시)
TRUNCATE TABLE item_views;
TRUNCATE TABLE cart_items;
TRUNCATE TABLE carts;
TRUNCATE TABLE user_coupons;
TRUNCATE TABLE order_items;
TRUNCATE TABLE orders;
TRUNCATE TABLE coupon_stocks;
TRUNCATE TABLE coupons;
TRUNCATE TABLE items;
TRUNCATE TABLE user_points;

-- 프로시저 실행 (순서대로)
SELECT '========================================' AS '';
SELECT 'Starting MASSIVE data generation...' AS '';
SELECT 'Total: 1M users, 1M items, 5M orders, ~15M order items, 50M views' AS '';
SELECT '========================================' AS '';

CALL generate_users();
CALL generate_items();
CALL generate_coupons();
CALL generate_orders();
CALL generate_order_items();
CALL generate_carts();
CALL generate_cart_items();
CALL generate_user_coupons();
CALL generate_item_views();

-- 설정 복원
SET unique_checks = 1;
SET foreign_key_checks = 1;
SET autocommit = 1;

SELECT '========================================' AS '';
SELECT 'All data generation completed!' AS '';
SELECT '========================================' AS '';

-- 데이터 건수 확인
SELECT 'user_points' AS table_name, COUNT(*) AS row_count FROM user_points
UNION ALL
SELECT 'items', COUNT(*) FROM items
UNION ALL
SELECT 'coupons', COUNT(*) FROM coupons
UNION ALL
SELECT 'coupon_stocks', COUNT(*) FROM coupon_stocks
UNION ALL
SELECT 'orders', COUNT(*) FROM orders
UNION ALL
SELECT 'order_items', COUNT(*) FROM order_items
UNION ALL
SELECT 'carts', COUNT(*) FROM carts
UNION ALL
SELECT 'cart_items', COUNT(*) FROM cart_items
UNION ALL
SELECT 'user_coupons', COUNT(*) FROM user_coupons
UNION ALL
SELECT 'item_views', COUNT(*) FROM item_views;

-- 주문 상태 분포 확인
SELECT 'Order Status Distribution:' AS '';
SELECT status, COUNT(*) AS count, ROUND(COUNT(*) * 100.0 / (SELECT COUNT(*) FROM orders), 2) AS percentage
FROM orders
GROUP BY status
ORDER BY count DESC;

-- 프로시저 정리
DROP PROCEDURE IF EXISTS generate_users;
DROP PROCEDURE IF EXISTS generate_items;
DROP PROCEDURE IF EXISTS generate_coupons;
DROP PROCEDURE IF EXISTS generate_orders;
DROP PROCEDURE IF EXISTS generate_order_items;
DROP PROCEDURE IF EXISTS generate_carts;
DROP PROCEDURE IF EXISTS generate_cart_items;
DROP PROCEDURE IF EXISTS generate_user_coupons;
DROP PROCEDURE IF EXISTS generate_item_views;
