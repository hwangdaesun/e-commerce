-- ============================================
-- 쿠폰 발급 데이터 정합성 검증 스크립트
-- ============================================
--
-- 목적: 쿠폰 선착순 발급 시스템의 데이터 정합성 검증
--
-- 검증 항목:
--   1. 쿠폰 오버 발급 검증 (발급 수량 > 총 수량)
--   2. 중복 발급 검증 (같은 사용자가 같은 쿠폰을 여러 번 발급받음)
--   3. 쿠폰 재고 일치 검증 (coupon_stocks.remaining_quantity 정합성)
--   4. 발급 통계
--   5. 최종 검증 요약
--
-- 실행 방법:
--   mysql -u {user} -p {database} < coupon-issue-integrity-check.sql
--
-- ============================================

SET @test_start_time = DATE_SUB(NOW(), INTERVAL 1 HOUR);  -- 최근 1시간 내 발급된 쿠폰 대상

-- ============================================
-- 1. 쿠폰 오버 발급 검증
-- ============================================

SELECT '============================================' AS '';
SELECT '1. 쿠폰 오버 발급 검증' AS '';
SELECT '============================================' AS '';
SELECT '' AS '';

SELECT '쿠폰별 발급 수량 vs 총 수량' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    c.coupon_id AS '쿠폰_ID',
    c.name AS '쿠폰명',
    c.total_quantity AS '총_수량',
    COUNT(uc.user_coupon_id) AS '발급_수량',
    c.total_quantity - COUNT(uc.user_coupon_id) AS '남은_수량',
    CASE
        WHEN COUNT(uc.user_coupon_id) > c.total_quantity THEN '❌ 오버 발급!'
        WHEN COUNT(uc.user_coupon_id) = c.total_quantity THEN '✅ 매진'
        ELSE '✅ 정상'
    END AS '상태'
FROM coupons c
LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
GROUP BY c.coupon_id, c.name, c.total_quantity
ORDER BY
    CASE
        WHEN COUNT(uc.user_coupon_id) > c.total_quantity THEN 1
        ELSE 2
    END,
    c.coupon_id;

SELECT '' AS '';
SELECT '오버 발급 집계' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(CASE WHEN 발급_수량 > 총_수량 THEN 1 END) AS '오버_발급_쿠폰_수',
    SUM(CASE WHEN 발급_수량 > 총_수량 THEN 발급_수량 - 총_수량 ELSE 0 END) AS '총_오버_발급_건수'
FROM (
    SELECT
        c.coupon_id,
        c.total_quantity AS 총_수량,
        COUNT(uc.user_coupon_id) AS 발급_수량
    FROM coupons c
    LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
    GROUP BY c.coupon_id, c.total_quantity
) AS coupon_stats;

SELECT '' AS '';

-- ============================================
-- 2. 중복 발급 검증
-- ============================================

SELECT '============================================' AS '';
SELECT '2. 중복 발급 검증' AS '';
SELECT '============================================' AS '';
SELECT '' AS '';

SELECT '사용자별 쿠폰 발급 횟수 (중복 발급 체크)' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    uc.user_id AS '사용자_ID',
    uc.coupon_id AS '쿠폰_ID',
    c.name AS '쿠폰명',
    COUNT(*) AS '발급_횟수',
    GROUP_CONCAT(uc.user_coupon_id ORDER BY uc.issued_at) AS '발급_ID_목록',
    MIN(uc.issued_at) AS '첫_발급_시간',
    MAX(uc.issued_at) AS '마지막_발급_시간'
FROM user_coupons uc
INNER JOIN coupons c ON uc.coupon_id = c.coupon_id
GROUP BY uc.user_id, uc.coupon_id, c.name
HAVING COUNT(*) > 1
ORDER BY COUNT(*) DESC
LIMIT 100;

SELECT '' AS '';
SELECT '중복 발급 집계' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(*) AS '중복_발급_건수',
    SUM(발급_횟수 - 1) AS '총_중복_발급_수'
FROM (
    SELECT
        user_id,
        coupon_id,
        COUNT(*) AS 발급_횟수
    FROM user_coupons
    GROUP BY user_id, coupon_id
    HAVING COUNT(*) > 1
) AS duplicate_stats;

SELECT '' AS '';

-- ============================================
-- 3. 쿠폰 재고 일치 검증
-- ============================================

SELECT '============================================' AS '';
SELECT '3. 쿠폰 재고 일치 검증' AS '';
SELECT '============================================' AS '';
SELECT '' AS '';

SELECT '쿠폰 재고 정합성 체크' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    c.coupon_id AS '쿠폰_ID',
    c.name AS '쿠폰명',
    c.total_quantity AS '총_수량',
    COUNT(uc.user_coupon_id) AS '실제_발급_수',
    c.total_quantity - COUNT(uc.user_coupon_id) AS '예상_남은_수량',
    cs.remaining_quantity AS 'DB_남은_수량',
    CASE
        WHEN cs.remaining_quantity = c.total_quantity - COUNT(uc.user_coupon_id) THEN '✅ 일치'
        ELSE '❌ 불일치'
    END AS '재고_상태'
FROM coupons c
LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
GROUP BY c.coupon_id, c.name, c.total_quantity, cs.remaining_quantity
ORDER BY
    CASE
        WHEN cs.remaining_quantity != c.total_quantity - COUNT(uc.user_coupon_id) THEN 1
        ELSE 2
    END,
    c.coupon_id;

SELECT '' AS '';
SELECT '재고 불일치 집계' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(CASE WHEN 재고_상태 = '❌ 불일치' THEN 1 END) AS '재고_불일치_쿠폰_수'
FROM (
    SELECT
        CASE
            WHEN cs.remaining_quantity = c.total_quantity - COUNT(uc.user_coupon_id) THEN '✅ 일치'
            ELSE '❌ 불일치'
        END AS 재고_상태
    FROM coupons c
    LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
    LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
    GROUP BY c.coupon_id, c.total_quantity, cs.remaining_quantity
) AS stock_stats;

SELECT '' AS '';

-- ============================================
-- 4. 발급 통계
-- ============================================

SELECT '============================================' AS '';
SELECT '4. 발급 통계' AS '';
SELECT '============================================' AS '';
SELECT '' AS '';

SELECT '전체 발급 현황' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    COUNT(DISTINCT c.coupon_id) AS '총_쿠폰_수',
    SUM(c.total_quantity) AS '총_발급_가능_수량',
    COUNT(uc.user_coupon_id) AS '총_발급_건수',
    COUNT(DISTINCT uc.user_id) AS '쿠폰_발급_받은_사용자_수',
    CONCAT(ROUND(COUNT(uc.user_coupon_id) * 100.0 / SUM(c.total_quantity), 2), '%') AS '발급률',
    SUM(cs.remaining_quantity) AS '총_남은_수량'
FROM coupons c
LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id;

SELECT '' AS '';
SELECT '쿠폰별 상위 발급 현황 (TOP 20)' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    c.coupon_id AS '쿠폰_ID',
    c.name AS '쿠폰명',
    c.discount_amount AS '할인_금액',
    c.total_quantity AS '총_수량',
    COUNT(uc.user_coupon_id) AS '발급_수량',
    cs.remaining_quantity AS '남은_수량',
    CONCAT(ROUND(COUNT(uc.user_coupon_id) * 100.0 / c.total_quantity, 2), '%') AS '발급률',
    CASE
        WHEN COUNT(uc.user_coupon_id) >= c.total_quantity THEN '✅ 매진'
        WHEN COUNT(uc.user_coupon_id) >= c.total_quantity * 0.9 THEN '⚠️ 거의 매진'
        ELSE '🟢 발급 가능'
    END AS '상태'
FROM coupons c
LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
GROUP BY c.coupon_id, c.name, c.discount_amount, c.total_quantity, cs.remaining_quantity
ORDER BY COUNT(uc.user_coupon_id) DESC
LIMIT 20;

SELECT '' AS '';
SELECT '사용자별 쿠폰 발급 현황 (TOP 20)' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    uc.user_id AS '사용자_ID',
    COUNT(*) AS '발급_받은_쿠폰_수',
    SUM(c.discount_amount) AS '총_할인_가능_금액',
    COUNT(CASE WHEN uc.is_used = 1 THEN 1 END) AS '사용한_쿠폰_수',
    COUNT(CASE WHEN uc.is_used = 0 THEN 1 END) AS '미사용_쿠폰_수',
    MIN(uc.issued_at) AS '첫_발급_시간',
    MAX(uc.issued_at) AS '마지막_발급_시간'
FROM user_coupons uc
INNER JOIN coupons c ON uc.coupon_id = c.coupon_id
GROUP BY uc.user_id
ORDER BY COUNT(*) DESC
LIMIT 20;

SELECT '' AS '';

-- ============================================
-- 5. 최종 검증 요약
-- ============================================

SELECT '============================================' AS '';
SELECT '5. 최종 검증 요약' AS '';
SELECT '============================================' AS '';
SELECT '' AS '';

-- 오버 발급 체크
SET @over_issue_count = (
    SELECT COUNT(*)
    FROM (
        SELECT c.coupon_id
        FROM coupons c
        LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
        GROUP BY c.coupon_id, c.total_quantity
        HAVING COUNT(uc.user_coupon_id) > c.total_quantity
    ) AS over_issued
);

-- 중복 발급 체크
SET @duplicate_issue_count = (
    SELECT COUNT(*)
    FROM (
        SELECT user_id, coupon_id
        FROM user_coupons
        GROUP BY user_id, coupon_id
        HAVING COUNT(*) > 1
    ) AS duplicates
);

-- 재고 불일치 체크
SET @stock_mismatch_count = (
    SELECT COUNT(*)
    FROM (
        SELECT c.coupon_id
        FROM coupons c
        LEFT JOIN user_coupons uc ON c.coupon_id = uc.coupon_id
        LEFT JOIN coupon_stocks cs ON c.coupon_id = cs.coupon_id
        GROUP BY c.coupon_id, c.total_quantity, cs.remaining_quantity
        HAVING cs.remaining_quantity != c.total_quantity - COUNT(uc.user_coupon_id)
    ) AS mismatches
);

SELECT
    CASE
        WHEN @over_issue_count = 0 THEN '✅ 통과'
        ELSE CONCAT('❌ 실패 (', @over_issue_count, '건)')
    END AS '오버_발급_검증',
    CASE
        WHEN @duplicate_issue_count = 0 THEN '✅ 통과'
        ELSE CONCAT('❌ 실패 (', @duplicate_issue_count, '건)')
    END AS '중복_발급_검증',
    CASE
        WHEN @stock_mismatch_count = 0 THEN '✅ 통과'
        ELSE CONCAT('❌ 실패 (', @stock_mismatch_count, '건)')
    END AS '재고_정합성_검증';

SELECT '' AS '';
SELECT '최종 결과' AS '';
SELECT '━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━' AS '';

SELECT
    CASE
        WHEN @over_issue_count = 0 AND @duplicate_issue_count = 0 AND @stock_mismatch_count = 0
        THEN '✅ 모든 검증 통과 - 데이터 정합성 양호'
        ELSE '❌ 검증 실패 - 데이터 정합성 문제 발견'
    END AS '검증_결과';

SELECT '' AS '';
SELECT '============================================' AS '';
SELECT '검증 완료' AS '';
SELECT '============================================' AS '';