-- ============================================
-- 부하 테스트 데이터 삭제 스크립트
-- ============================================
--
-- 목적: 테스트 후 생성된 데이터 정리
-- 주의: 모든 테스트 데이터가 삭제됩니다!
--
-- 실행 방법:
--   mysql -u root -p ecommerce < cleanup.sql
-- ============================================

USE ecommerce;

SELECT '========================================' AS '';
SELECT '⚠️  테스트 데이터 삭제를 시작합니다' AS '';
SELECT '========================================' AS '';

-- Foreign Key 체크 비활성화
SET foreign_key_checks = 0;

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
TRUNCATE TABLE ecommerce.items;

-- 상품 조회 데이터 삭제
SELECT '상품 데이터 삭제 중...' AS status;
TRUNCATE TABLE item_views;

-- 포인트 데이터 삭제
SELECT '포인트 데이터 삭제 중...' AS status;
TRUNCATE TABLE user_points;

-- 사용자 데이터 삭제
SELECT '사용자 데이터 삭제 중...' AS status;
TRUNCATE TABLE users;

-- Foreign Key 체크 활성화
SET foreign_key_checks = 1;

-- 삭제 확인
SELECT '========================================' AS '';
SELECT '✅ 데이터 삭제 완료' AS '';
SELECT '========================================' AS '';

SELECT '테이블별 남은 데이터 건수' AS category, '' AS count
UNION ALL
SELECT '─────────────────', '──────────'
UNION ALL
SELECT 'users', CONCAT(COUNT(*), '건') FROM users
UNION ALL
SELECT 'user_points', CONCAT(COUNT(*), '건') FROM user_points
UNION ALL
SELECT 'items', CONCAT(COUNT(*), '건') FROM items
UNION ALL
SELECT 'coupons', CONCAT(COUNT(*), '건') FROM coupons
UNION ALL
SELECT 'coupon_stocks', CONCAT(COUNT(*), '건') FROM coupon_stocks
UNION ALL
SELECT 'user_coupons', CONCAT(COUNT(*), '건') FROM user_coupons
UNION ALL
SELECT 'carts', CONCAT(COUNT(*), '건') FROM carts
UNION ALL
SELECT 'cart_items', CONCAT(COUNT(*), '건') FROM cart_items
UNION ALL
SELECT 'orders', CONCAT(COUNT(*), '건') FROM orders
UNION ALL
SELECT 'order_items', CONCAT(COUNT(*), '건') FROM order_items;

SELECT '========================================' AS '';
