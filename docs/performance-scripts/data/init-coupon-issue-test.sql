-- ============================================
-- 쿠폰 발급 부하 테스트 데이터 초기화 스크립트
-- ============================================
--
-- 목적: 쿠폰 선착순 발급 부하 테스트를 위한 테스트 데이터 생성
--
-- 생성 데이터:
--   - 쿠폰: 100개 (다양한 수량과 할인 금액)
--   - 쿠폰 재고: 100개
--   - 사용자: 기존 users 테이블 사용 (100만 명)
--
-- 실행 방법:
--   mysql -u {user} -p {database} < init-coupon-issue-test.sql
--
-- 소요 시간: 약 1~2분
--
-- ============================================

SET autocommit = 0;
SET foreign_key_checks = 0;

-- ============================================
-- 1. 기존 쿠폰 데이터 정리
-- ============================================

SELECT '==========================================' AS '';
SELECT '1. 기존 쿠폰 데이터 정리 중...' AS '';
SELECT '==========================================' AS '';

-- 발급된 쿠폰 삭제
TRUNCATE TABLE user_coupons;

-- 쿠폰 재고 삭제
TRUNCATE TABLE coupon_stocks;

-- 쿠폰 삭제
TRUNCATE TABLE coupons;

SELECT '✅ 기존 데이터 정리 완료' AS '';
SELECT '' AS '';

COMMIT;

-- ============================================
-- 2. 쿠폰 생성 (100개)
-- ============================================

SELECT '==========================================' AS '';
SELECT '2. 쿠폰 생성 중... (100개)' AS '';
SELECT '==========================================' AS '';

-- 다양한 할인 금액 및 수량의 쿠폰 생성
-- 1~20번: 할인 1,000원, 수량 100개 (인기 쿠폰, 경쟁 치열)
-- 21~40번: 할인 3,000원, 수량 500개 (중간 경쟁)
-- 41~60번: 할인 5,000원, 수량 1,000개 (보통 쿠폰)
-- 61~80번: 할인 10,000원, 수량 2,000개 (대용량)
-- 81~100번: 할인 20,000원, 수량 5,000개 (대용량, 낮은 경쟁)

INSERT INTO coupons (name, discount_amount, total_quantity, expires_at, created_at, updated_at)
SELECT
    CONCAT('부하테스트_쿠폰_', seq) AS name,
    CASE
        WHEN seq <= 20 THEN 1000
        WHEN seq <= 40 THEN 3000
        WHEN seq <= 60 THEN 5000
        WHEN seq <= 80 THEN 10000
        ELSE 20000
    END AS discount_amount,
    CASE
        WHEN seq <= 20 THEN 100
        WHEN seq <= 40 THEN 500
        WHEN seq <= 60 THEN 1000
        WHEN seq <= 80 THEN 2000
        ELSE 5000
    END AS total_quantity,
    DATE_ADD(NOW(), INTERVAL 30 DAY) AS expires_at,
    NOW() AS created_at,
    NOW() AS updated_at
FROM (
    SELECT (@row_number := @row_number + 1) AS seq
    FROM information_schema.columns,
    (SELECT @row_number := 0) AS init
    LIMIT 100
) AS numbers;

COMMIT;

SELECT CONCAT('✅ 쿠폰 ', COUNT(*), '개 생성 완료') AS ''
FROM coupons;

SELECT '' AS '';

-- ============================================
-- 3. 쿠폰 재고 초기화
-- ============================================

SELECT '==========================================' AS '';
SELECT '3. 쿠폰 재고 초기화 중...' AS '';
SELECT '==========================================' AS '';

INSERT INTO coupon_stocks (coupon_id, remaining_quantity, updated_at)
SELECT
    coupon_id,
    total_quantity AS remaining_quantity,
    NOW() AS updated_at
FROM coupons;

COMMIT;

SELECT CONCAT('✅ 쿠폰 재고 ', COUNT(*), '개 초기화 완료') AS ''
FROM coupon_stocks;

SELECT '' AS '';

-- ============================================
-- 4. 생성된 데이터 확인
-- ============================================

SELECT '==========================================' AS '';
SELECT '4. 생성된 데이터 확인' AS '';
SELECT '==========================================' AS '';
SELECT '' AS '';

SELECT '쿠폰 생성 현황' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    CASE
        WHEN discount_amount = 1000 THEN '1,000원 (인기)'
        WHEN discount_amount = 3000 THEN '3,000원 (중간)'
        WHEN discount_amount = 5000 THEN '5,000원 (보통)'
        WHEN discount_amount = 10000 THEN '10,000원 (대용량)'
        ELSE '20,000원 (대용량)'
    END AS '할인_금액_그룹',
    COUNT(*) AS '쿠폰_수',
    MIN(total_quantity) AS '최소_수량',
    MAX(total_quantity) AS '최대_수량',
    SUM(total_quantity) AS '총_발급_가능_수량'
FROM coupons
GROUP BY discount_amount
ORDER BY discount_amount;

SELECT '' AS '';
SELECT '전체 통계' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(*) AS '총_쿠폰_수',
    SUM(total_quantity) AS '총_발급_가능_수량',
    MIN(total_quantity) AS '최소_수량',
    MAX(total_quantity) AS '최대_수량',
    ROUND(AVG(total_quantity)) AS '평균_수량',
    MIN(discount_amount) AS '최소_할인_금액',
    MAX(discount_amount) AS '최대_할인_금액'
FROM coupons;

SELECT '' AS '';
SELECT '쿠폰 재고 확인' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(*) AS '재고_레코드_수',
    SUM(remaining_quantity) AS '총_남은_수량',
    MIN(remaining_quantity) AS '최소_재고',
    MAX(remaining_quantity) AS '최대_재고'
FROM coupon_stocks;

SELECT '' AS '';

-- ============================================
-- 5. 샘플 데이터 미리보기
-- ============================================

SELECT '==========================================' AS '';
SELECT '5. 샘플 쿠폰 미리보기 (각 그룹별 1개)' AS '';
SELECT '==========================================' AS '';
SELECT '' AS '';

(SELECT
    c.coupon_id AS '쿠폰_ID',
    c.name AS '쿠폰명',
    c.discount_amount AS '할인_금액',
    c.total_quantity AS '총_수량',
    cs.remaining_quantity AS '남은_수량',
    c.expires_at AS '만료일시'
FROM coupons c
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
WHERE c.discount_amount = 1000
LIMIT 1)

UNION ALL

(SELECT
    c.coupon_id,
    c.name,
    c.discount_amount,
    c.total_quantity,
    cs.remaining_quantity,
    c.expires_at
FROM coupons c
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
WHERE c.discount_amount = 3000
LIMIT 1)

UNION ALL

(SELECT
    c.coupon_id,
    c.name,
    c.discount_amount,
    c.total_quantity,
    cs.remaining_quantity,
    c.expires_at
FROM coupons c
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
WHERE c.discount_amount = 5000
LIMIT 1)

UNION ALL

(SELECT
    c.coupon_id,
    c.name,
    c.discount_amount,
    c.total_quantity,
    cs.remaining_quantity,
    c.expires_at
FROM coupons c
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
WHERE c.discount_amount = 10000
LIMIT 1)

UNION ALL

(SELECT
    c.coupon_id,
    c.name,
    c.discount_amount,
    c.total_quantity,
    cs.remaining_quantity,
    c.expires_at
FROM coupons c
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
WHERE c.discount_amount = 20000
LIMIT 1);

SELECT '' AS '';

-- ============================================
-- 완료
-- ============================================

SET foreign_key_checks = 1;
SET autocommit = 1;

SELECT '==========================================' AS '';
SELECT '✅ 쿠폰 발급 테스트 데이터 초기화 완료!' AS '';
SELECT '==========================================' AS '';
SELECT '' AS '';
SELECT '📋 다음 단계:' AS '';
SELECT '  1. K6 부하 테스트 실행:' AS '';
SELECT '     k6 run coupon-issue-basic-test.js' AS '';
SELECT '' AS '';
SELECT '  2. Consumer Lag 모니터링:' AS '';
SELECT '     ./monitor-consumer-lag.sh' AS '';
SELECT '' AS '';
SELECT '  3. 데이터 정합성 검증:' AS '';
SELECT '     mysql < validation/coupon-issue-integrity-check.sql' AS '';
SELECT '' AS '';
SELECT '==========================================' AS '';