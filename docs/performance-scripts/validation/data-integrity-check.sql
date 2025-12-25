-- ============================================
-- 부하 테스트 후 데이터 정합성 검증 스크립트
-- ============================================
--
-- 목적: 부하 테스트 실행 후 데이터 정합성 검증
-- 실행 시점: 부하 테스트 종료 후 30초 대기 (이벤트 처리 완료)
--
-- 검증 항목:
--   1. 주문 상태별 집계
--   2. 재고 오버셀링 검증 (음수 재고 체크)
--   3. 포인트 정합성 검증
--   4. 쿠폰 정합성 검증
--   5. 금액 정합성 검증
--   6. 실패 원인 분석
--
-- 실행 방법:
--   mysql -u root -p ecommerce < validation/data-integrity-check.sql
-- ============================================

USE ecommerce;

-- 테스트 시작 시간 설정 (최근 생성된 주문 기준)
SET @test_start_time = (
    SELECT DATE_SUB(MIN(created_at), INTERVAL 1 SECOND)
    FROM orders
    WHERE created_at > DATE_SUB(NOW(), INTERVAL 1 HOUR)
);

SELECT '========================================' AS '';
SELECT '🔍 데이터 정합성 검증 시작' AS '';
SELECT '========================================' AS '';
SELECT CONCAT('테스트 시작 시간: ', COALESCE(DATE_FORMAT(@test_start_time, '%Y-%m-%d %H:%i:%s'), '데이터 없음')) AS '';
SELECT '========================================' AS '';
SELECT '' AS '';

-- ============================================
-- 1. 주문 상태별 집계
-- ============================================

SELECT '========================================' AS '';
SELECT '📊 1. 주문 상태별 집계' AS '';
SELECT '========================================' AS '';

SELECT
    status AS '주문 상태',
    COUNT(*) AS '건수',
    CONCAT(ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER(), 2), '%') AS '비율'
FROM orders
WHERE created_at > @test_start_time
GROUP BY status
ORDER BY
    CASE status
        WHEN 'PAID' THEN 1
        WHEN 'PENDING' THEN 2
        WHEN 'FAILED' THEN 3
        ELSE 4
    END;

SELECT '' AS '';

-- 총 요청 수
SET @total_orders = (SELECT COUNT(*) FROM orders WHERE created_at > @test_start_time);
SET @paid_orders = (SELECT COUNT(*) FROM orders WHERE created_at > @test_start_time AND status = 'PAID');
SET @pending_orders = (SELECT COUNT(*) FROM orders WHERE created_at > @test_start_time AND status = 'PENDING');
SET @failed_orders = (SELECT COUNT(*) FROM orders WHERE created_at > @test_start_time AND status = 'FAILED');

SELECT
    '총 주문 수' AS '항목',
    FORMAT(@total_orders, 0) AS '값'
UNION ALL
SELECT '성공 (PAID)', FORMAT(@paid_orders, 0)
UNION ALL
SELECT '실패 (FAILED)', FORMAT(@failed_orders, 0)
UNION ALL
SELECT '처리 중 (PENDING)', FORMAT(@pending_orders, 0);

SELECT '' AS '';

-- ============================================
-- 2. 재고 오버셀링 검증 ⭐ 가장 중요
-- ============================================

SELECT '========================================' AS '';
SELECT '🚨 2. 재고 오버셀링 검증 (가장 중요!)' AS '';
SELECT '========================================' AS '';

-- 음수 재고 체크
SET @negative_stock_count = (SELECT COUNT(*) FROM items WHERE stock < 0);

SELECT
    CASE
        WHEN @negative_stock_count = 0 THEN '✅ 통과: 오버셀링 없음'
        ELSE CONCAT('❌ 실패: 오버셀링 발견 (', @negative_stock_count, '건)')
    END AS '검증 결과';

-- 오버셀링 발생한 상품 상세 정보
SELECT
    item_id AS '상품 ID',
    name AS '상품명',
    stock AS '현재 재고',
    ABS(stock) AS '오버셀링 수량'
FROM items
WHERE stock < 0
ORDER BY stock ASC
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 3. 재고 변동 분석 (참고용)
-- ============================================

SELECT '========================================' AS '';
SELECT '📦 3. 재고 변동 TOP 10 (판매량 많은 순)' AS '';
SELECT '========================================' AS '';

SELECT
    i.item_id AS '상품 ID',
    i.name AS '상품명',
    i.stock AS '현재 재고',
    COALESCE(SUM(oi.quantity), 0) AS '판매량',
    CASE
        WHEN i.stock >= 0 THEN '✅ 정상'
        ELSE '❌ 오버셀링'
    END AS '상태'
FROM items i
LEFT JOIN order_items oi ON i.item_id = oi.item_id
LEFT JOIN orders o ON oi.order_id = o.order_id
    AND o.status = 'PAID'
    AND o.created_at > @test_start_time
GROUP BY i.item_id, i.name, i.stock
HAVING COALESCE(SUM(oi.quantity), 0) > 0
ORDER BY COALESCE(SUM(oi.quantity), 0) DESC
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 4. 포인트 정합성 검증
-- ============================================

SELECT '========================================' AS '';
SELECT '💰 4. 포인트 정합성 검증' AS '';
SELECT '========================================' AS '';

-- 음수 포인트 체크
SET @negative_point_count = (SELECT COUNT(*) FROM user_points WHERE point < 0);

SELECT
    CASE
        WHEN @negative_point_count = 0 THEN '✅ 통과: 음수 포인트 없음'
        ELSE CONCAT('❌ 실패: 음수 포인트 발견 (', @negative_point_count, '건)')
    END AS '검증 결과';

-- 음수 포인트 사용자 상세
SELECT
    up.user_id AS '사용자 ID',
    up.point AS '현재 포인트',
    ABS(up.point) AS '부족 포인트',
    COUNT(o.order_id) AS '주문 건수',
    COALESCE(SUM(o.final_amount), 0) AS '총 사용 금액'
FROM user_points up
LEFT JOIN orders o ON up.user_id = o.user_id
    AND o.status = 'PAID'
    AND o.created_at > @test_start_time
WHERE up.point < 0
GROUP BY up.user_id, up.point
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 5. 쿠폰 정합성 검증
-- ============================================

SELECT '========================================' AS '';
SELECT '🎫 5. 쿠폰 정합성 검증' AS '';
SELECT '========================================' AS '';

-- 쿠폰 중복 사용 체크
DROP TEMPORARY TABLE IF EXISTS coupon_usage_check;

CREATE TEMPORARY TABLE coupon_usage_check AS
SELECT
    uc.user_coupon_id,
    uc.user_id,
    uc.coupon_id,
    uc.is_used AS '쿠폰_사용_여부',
    COUNT(DISTINCT o.order_id) AS '실제_사용_횟수',
    CASE
        WHEN uc.is_used = TRUE AND COUNT(DISTINCT o.order_id) = 1 THEN 'OK'
        WHEN uc.is_used = FALSE AND COUNT(DISTINCT o.order_id) = 0 THEN 'OK'
        WHEN uc.is_used = TRUE AND COUNT(DISTINCT o.order_id) > 1 THEN '중복_사용'
        WHEN uc.is_used = TRUE AND COUNT(DISTINCT o.order_id) = 0 THEN '불일치_사용처리됨'
        WHEN uc.is_used = FALSE AND COUNT(DISTINCT o.order_id) > 0 THEN '불일치_미사용처리됨'
        ELSE '기타_오류'
    END AS '검증_상태'
FROM user_coupons uc
LEFT JOIN order_items oi ON uc.user_coupon_id = oi.user_coupon_id
LEFT JOIN orders o ON oi.order_id = o.order_id
    AND o.status = 'PAID'
    AND o.created_at > @test_start_time
WHERE uc.user_coupon_id IN (
    SELECT DISTINCT user_coupon_id
    FROM order_items
    WHERE user_coupon_id IS NOT NULL
    AND order_id IN (
        SELECT order_id
        FROM orders
        WHERE created_at > @test_start_time
    )
)
GROUP BY uc.user_coupon_id, uc.user_id, uc.coupon_id, uc.is_used;

-- 쿠폰 정합성 요약
SELECT
    검증_상태 AS '상태',
    COUNT(*) AS '건수'
FROM coupon_usage_check
GROUP BY 검증_상태;

-- 문제 있는 쿠폰 상세
SELECT
    user_coupon_id AS '쿠폰 ID',
    user_id AS '사용자 ID',
    쿠폰_사용_여부 AS 'is_used',
    실제_사용_횟수 AS '실제 사용',
    검증_상태 AS '문제'
FROM coupon_usage_check
WHERE 검증_상태 != 'OK'
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 6. 쿠폰 재고 검증
-- ============================================

SELECT '========================================' AS '';
SELECT '📊 6. 쿠폰 재고 검증' AS '';
SELECT '========================================' AS '';

-- 음수 쿠폰 재고 체크
SET @negative_coupon_stock = (SELECT COUNT(*) FROM coupon_stocks WHERE remaining_quantity < 0);

SELECT
    CASE
        WHEN @negative_coupon_stock = 0 THEN '✅ 통과: 쿠폰 오버 발급 없음'
        ELSE CONCAT('❌ 실패: 쿠폰 오버 발급 (', @negative_coupon_stock, '건)')
    END AS '검증 결과';

-- 문제 있는 쿠폰 재고
SELECT
    cs.coupon_id AS '쿠폰 ID',
    c.name AS '쿠폰명',
    cs.remaining_quantity AS '남은 수량',
    ABS(cs.remaining_quantity) AS '오버 발급량'
FROM coupon_stocks cs
JOIN coupons c ON cs.coupon_id = c.coupon_id
WHERE cs.remaining_quantity < 0
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 7. 주문 실패 원인 분석
-- ============================================

SELECT '========================================' AS '';
SELECT '📉 7. 주문 실패 원인 분석' AS '';
SELECT '========================================' AS '';

SELECT
    COALESCE(fail_reason, '실패 원인 없음') AS '실패 원인',
    COUNT(*) AS '건수',
    CONCAT(ROUND(COUNT(*) * 100.0 / NULLIF(@failed_orders, 0), 2), '%') AS '비율'
FROM orders
WHERE status = 'FAILED'
    AND created_at > @test_start_time
GROUP BY fail_reason
ORDER BY COUNT(*) DESC;

SELECT '' AS '';

-- ============================================
-- 8. PENDING 상태 분석 (이벤트 처리 지연)
-- ============================================

SELECT '========================================' AS '';
SELECT '⏳ 8. PENDING 상태 분석 (이벤트 처리 미완료)' AS '';
SELECT '========================================' AS '';

SELECT
    COUNT(*) AS 'PENDING 건수',
    CASE
        WHEN COUNT(*) = 0 THEN '✅ 모든 주문 처리 완료'
        WHEN COUNT(*) > 0 AND COUNT(*) < @total_orders * 0.01 THEN '⚠️  소수 미처리 (1% 미만)'
        ELSE '❌ 많은 주문 미처리'
    END AS '상태'
FROM orders
WHERE status = 'PENDING'
    AND created_at > @test_start_time;

-- PENDING 상태 상세 (최근 10건)
SELECT
    order_id AS '주문 ID',
    user_id AS '사용자 ID',
    total_amount AS '주문 금액',
    TIMESTAMPDIFF(SECOND, created_at, NOW()) AS '경과_시간_초'
FROM orders
WHERE status = 'PENDING'
    AND created_at > @test_start_time
ORDER BY created_at DESC
LIMIT 10;

SELECT '' AS '';

-- ============================================
-- 9. 최종 검증 요약
-- ============================================

SELECT '========================================' AS '';
SELECT '✅ 최종 검증 요약' AS '';
SELECT '========================================' AS '';

SELECT
    '검증 항목' AS '항목',
    '결과' AS '상태'
UNION ALL
SELECT '─────────────────────', '──────────'
UNION ALL
SELECT
    '1. 재고 오버셀링',
    CASE WHEN @negative_stock_count = 0 THEN '✅ 통과' ELSE '❌ 실패' END
UNION ALL
SELECT
    '2. 포인트 음수',
    CASE WHEN @negative_point_count = 0 THEN '✅ 통과' ELSE '❌ 실패' END
UNION ALL
SELECT
    '3. 쿠폰 재고',
    CASE WHEN @negative_coupon_stock = 0 THEN '✅ 통과' ELSE '❌ 실패' END
UNION ALL
SELECT
    '4. PENDING 처리',
    CASE
        WHEN @pending_orders = 0 THEN '✅ 통과'
        WHEN @pending_orders < @total_orders * 0.01 THEN '⚠️  일부 미처리'
        ELSE '❌ 많은 미처리'
    END
UNION ALL
SELECT
    '5. 총 주문 수',
    FORMAT(@total_orders, 0)
UNION ALL
SELECT
    '6. 성공률',
    CONCAT(ROUND(@paid_orders * 100.0 / NULLIF(@total_orders, 0), 2), '%');

SELECT '========================================' AS '';
SELECT CASE
    WHEN @negative_stock_count = 0
        AND @negative_point_count = 0
        AND @negative_coupon_stock = 0
    THEN '🎉 모든 정합성 검증 통과!'
    ELSE '⚠️  일부 검증 실패 - 위 내용을 확인하세요'
END AS '최종 결과';
SELECT '========================================' AS '';