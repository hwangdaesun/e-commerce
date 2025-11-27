-- 최근 3일간 인기 상품 테스트 데이터 생성 프로시저
-- 기존 items 테이블의 상품을 랜덤으로 선택하여 조회/구매 데이터를 생성합니다.

USE ecommerce;

DELIMITER //

-- 1. 상품 조회 데이터 생성 프로시저
DROP PROCEDURE IF EXISTS generate_recent_item_views //
CREATE PROCEDURE generate_recent_item_views(
    IN view_count INT,           -- 생성할 조회 수
    IN days_back INT             -- 며칠 전부터 데이터 생성 (기본 3일)
)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE random_item_id BIGINT;
    DECLARE random_user_id BIGINT;
    DECLARE random_timestamp DATETIME;
    DECLARE total_items BIGINT;
    DECLARE total_users BIGINT;

    -- 기존 상품 수 확인
    SELECT COUNT(*) INTO total_items FROM items;
    IF total_items = 0 THEN
        SELECT 'Error: No items found in database' AS message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No items found';
    END IF;

    -- 기존 사용자 수 확인
    SELECT COUNT(*) INTO total_users FROM user_points;
    IF total_users = 0 THEN
        SELECT 'Error: No users found in database' AS message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No users found';
    END IF;

    SELECT CONCAT('Generating ', view_count, ' item views for last ', days_back, ' days...') AS message;
    SELECT CONCAT('Total items: ', total_items, ', Total users: ', total_users) AS info;

    WHILE i <= view_count DO
        -- 랜덤 상품 선택 (1 ~ total_items)
        SET random_item_id = FLOOR(1 + RAND() * total_items);

        -- 랜덤 사용자 선택 (1 ~ total_users)
        SET random_user_id = FLOOR(1 + RAND() * total_users);

        -- 랜덤 타임스탬프 (현재 ~ days_back일 전)
        SET random_timestamp = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * days_back * 24 * 60) MINUTE);

        -- ItemView 데이터 삽입
        INSERT INTO item_views (item_id, user_id, created_at, updated_at)
        VALUES (random_item_id, random_user_id, random_timestamp, random_timestamp);

        SET i = i + 1;

        -- 진행상황 출력 (1000건마다)
        IF i % 1000 = 0 THEN
            SELECT CONCAT(i, ' item views generated...') AS progress;
        END IF;
    END WHILE;

    SELECT CONCAT(view_count, ' item views generated successfully!') AS message;
END //

-- 2. 상품 구매 데이터 생성 프로시저
DROP PROCEDURE IF EXISTS generate_recent_orders //
CREATE PROCEDURE generate_recent_orders(
    IN order_count INT,          -- 생성할 주문 수
    IN days_back INT,            -- 며칠 전부터 데이터 생성 (기본 3일)
    IN min_items_per_order INT,  -- 주문당 최소 상품 수 (기본 1)
    IN max_items_per_order INT   -- 주문당 최대 상품 수 (기본 3)
)
BEGIN
    DECLARE i INT DEFAULT 1;
    DECLARE j INT;
    DECLARE items_in_order INT;
    DECLARE random_item_id BIGINT;
    DECLARE random_user_id BIGINT;
    DECLARE random_timestamp DATETIME;
    DECLARE total_items BIGINT;
    DECLARE total_users BIGINT;
    DECLARE current_order_id BIGINT;
    DECLARE item_price INT;
    DECLARE item_quantity INT;
    DECLARE order_total_amount INT;
    DECLARE coupon_discount INT;
    DECLARE item_name VARCHAR(255);

    -- 기존 상품 수 확인
    SELECT COUNT(*) INTO total_items FROM items;
    IF total_items = 0 THEN
        SELECT 'Error: No items found in database' AS message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No items found';
    END IF;

    -- 기존 사용자 수 확인
    SELECT COUNT(*) INTO total_users FROM user_points;
    IF total_users = 0 THEN
        SELECT 'Error: No users found in database' AS message;
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'No users found';
    END IF;

    SELECT CONCAT('Generating ', order_count, ' orders for last ', days_back, ' days...') AS message;
    SELECT CONCAT('Items per order: ', min_items_per_order, '-', max_items_per_order) AS info;

    WHILE i <= order_count DO
        -- 랜덤 사용자 선택
        SET random_user_id = FLOOR(1 + RAND() * total_users);

        -- 랜덤 타임스탬프 (현재 ~ days_back일 전)
        SET random_timestamp = DATE_SUB(NOW(), INTERVAL FLOOR(RAND() * days_back * 24 * 60) MINUTE);

        -- 주문당 상품 수 결정
        SET items_in_order = FLOOR(min_items_per_order + RAND() * (max_items_per_order - min_items_per_order + 1));

        -- 주문 총액 초기화
        SET order_total_amount = 0;

        -- 임시로 Order 생성 (총액은 나중에 업데이트)
        SET coupon_discount = IF(RAND() > 0.7, FLOOR(RAND() * 5000), 0);  -- 30% 확률로 쿠폰 사용

        INSERT INTO orders (user_id, status, total_amount, coupon_discount, final_amount, created_at, updated_at)
        VALUES (random_user_id, 'PAID', 0, coupon_discount, 0, random_timestamp, random_timestamp);

        SET current_order_id = LAST_INSERT_ID();

        -- OrderItem 생성
        SET j = 1;
        WHILE j <= items_in_order DO
            -- 랜덤 상품 선택
            SET random_item_id = FLOOR(1 + RAND() * total_items);

            -- 상품 정보 가져오기
            SELECT name, price INTO item_name, item_price
            FROM items
            WHERE item_id = random_item_id
            LIMIT 1;

            -- 수량 결정 (1-3개)
            SET item_quantity = FLOOR(1 + RAND() * 3);

            -- OrderItem 삽입
            INSERT INTO order_items (order_id, item_id, name, price, quantity, user_coupon_id)
            VALUES (current_order_id, random_item_id, item_name, item_price, item_quantity, NULL);

            -- 주문 총액 누적
            SET order_total_amount = order_total_amount + (item_price * item_quantity);

            SET j = j + 1;
        END WHILE;

        -- Order 총액 업데이트
        UPDATE orders
        SET total_amount = order_total_amount,
            final_amount = GREATEST(order_total_amount - coupon_discount, 0)
        WHERE order_id = current_order_id;

        SET i = i + 1;

        -- 진행상황 출력 (100건마다)
        IF i % 100 = 0 THEN
            SELECT CONCAT(i, ' orders generated...') AS progress;
        END IF;
    END WHILE;

    SELECT CONCAT(order_count, ' orders generated successfully!') AS message;
END //

-- 3. 통합 프로시저: 조회 + 구매 데이터 한번에 생성
DROP PROCEDURE IF EXISTS generate_popular_items_test_data //
CREATE PROCEDURE generate_popular_items_test_data(
    IN view_count INT,           -- 생성할 조회 수
    IN order_count INT,          -- 생성할 주문 수
    IN days_back INT             -- 며칠 전부터 데이터 생성 (기본 3일)
)
BEGIN
    SELECT '========================================' AS '';
    SELECT 'Generating popular items test data...' AS '';
    SELECT CONCAT('Views: ', view_count, ', Orders: ', order_count, ', Days: ', days_back) AS '';
    SELECT '========================================' AS '';

    -- 조회 데이터 생성
    CALL generate_recent_item_views(view_count, days_back);

    -- 구매 데이터 생성
    CALL generate_recent_orders(order_count, days_back, 1, 3);

    SELECT '========================================' AS '';
    SELECT 'Test data generation completed!' AS '';
    SELECT '========================================' AS '';

    -- 생성된 데이터 확인
    SELECT 'Recent Item Views (last 3 days):' AS '';
    SELECT COUNT(*) AS total_views
    FROM item_views
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL days_back DAY);

    SELECT 'Recent Orders (last 3 days):' AS '';
    SELECT COUNT(*) AS total_orders, SUM(final_amount) AS total_sales
    FROM orders
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL days_back DAY) AND status = 'PAID';

    SELECT 'Top 10 Most Viewed Items (last 3 days):' AS '';
    SELECT item_id, COUNT(*) AS view_count
    FROM item_views
    WHERE created_at >= DATE_SUB(NOW(), INTERVAL days_back DAY)
    GROUP BY item_id
    ORDER BY view_count DESC
    LIMIT 10;

    SELECT 'Top 10 Best Selling Items (last 3 days):' AS '';
    SELECT oi.item_id, SUM(oi.quantity) AS total_quantity
    FROM order_items oi
    JOIN orders o ON oi.order_id = o.order_id
    WHERE o.created_at >= DATE_SUB(NOW(), INTERVAL days_back DAY) AND o.status = 'PAID'
    GROUP BY oi.item_id
    ORDER BY total_quantity DESC
    LIMIT 10;
END //

DELIMITER ;

CALL generate_recent_item_views(1000, 3);


CALL generate_popular_items_test_data(5000, 1000, 3);

-- ========================================
-- 사용 예제
-- ========================================

/*
-- 예제 1: 최근 3일간 조회 1000건 생성
CALL generate_recent_item_views(1000, 3);

-- 예제 2: 최근 3일간 주문 500건 생성 (주문당 1-3개 상품)
CALL generate_recent_orders(500, 3, 1, 3);

-- 예제 3: 조회 + 구매 데이터 한번에 생성
CALL generate_popular_items_test_data(5000, 1000, 3);

-- 예제 4: 최근 7일간 대량 데이터 생성
CALL generate_popular_items_test_data(50000, 10000, 7);

-- 생성된 데이터 확인
SELECT COUNT(*) FROM item_views WHERE created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY);
SELECT COUNT(*) FROM orders WHERE created_at >= DATE_SUB(NOW(), INTERVAL 3 DAY) AND status = 'PAID';
*/
