# 데이터 모델

## 1. 상위 자원 분류

### 1.1 상품 도메인 (Item Domain)
상품 및 옵션 관련 자원

### 1.2 사용자 도메인 (User Domain)
사용자 및 장바구니 관련 자원

### 1.3 주문 도메인 (Order Domain)
주문 및 주문 관련 자원

### 1.4 쿠폰 도메인 (Coupon Domain)
쿠폰 및 쿠폰 관련 자원

---

## 2. 자원 목록

### 2.1 상품 도메인 (Item Domain)

#### Item (상품)
- `itemId`: Long (PK)
- `name`: String (상품명)
- `price`: Integer (가격)
- `createdAt`: DateTime (생성일시)
- `updatedAt`: DateTime (수정일시)

#### ItemStock (상품 재고)
- `itemId`: Long (PK, FK) - Item과 식별 관계
- `stock`: Integer (재고 수량)
- `updatedAt`: DateTime (수정일시)
---

### 2.2 사용자 도메인 (User Domain)

#### User (사용자)
- `userId`: Long (PK)
- `name`: String (사용자명)
- `createdAt`: DateTime (생성일시)

#### UserPoint (사용자 포인트)
- `userId`: Long (PK, FK) - User와 식별 관계
- `point`: Integer (보유 포인트)
- `updatedAt`: DateTime (수정일시)

#### UserPointHistory (사용자 포인트 이력)
- `pointHistoryId`: Long (PK)
- `userId`: Long
- `orderId`: Long (nullable)
- `amount`: Integer (변동 금액 - 양수: 충전, 음수: 사용)
- `balanceAfter`: Integer (변동 후 잔액)
- `type`: String (거래 유형 - CHARGE, USE)
- `createdAt`: DateTime (거래일시)

#### Cart (장바구니)
- `cartId`: Long (PK)
- `userId`: Long
- `createdAt`: DateTime (생성일시)

#### CartItem (장바구니 항목)
- `cartItemId`: Long (PK)
- `cartId`: Long
- `itemId`: Long
- `quantity`: Integer (수량)
- `createdAt`: DateTime (생성일시)
- `updatedAt`: DateTime (수정일시)

---

### 2.3 주문 도메인 (Order Domain)

#### Order (주문)
- `orderId`: Long (PK)
- `userId`: Long
- `status`: String (주문 상태 - PENDING, PAID, FAILED)
- `totalAmount`: Integer (총 상품 금액)
- `couponDiscount`: Integer (쿠폰 할인 금액, 0 if not used)
- `finalAmount`: Integer (최종 결제 금액 - 쿠폰 적용 후)
- `orderedAt`: DateTime (주문일시)

#### OrderItem (주문 항목)
- `orderItemId`: Long (PK)
- `orderId`: Long
- `cartItemId`: Long
- `itemId`: Long
- `name`: String (상품명)
- `price`: Integer (주문 가격)
- `quantity`: Integer (수량)
- `userCouponId`: Long (사용자 쿠폰 ID, nullable)

---

### 2.4 쿠폰 도메인 (Coupon Domain)

#### Coupon (쿠폰)
- `couponId`: Long (PK)
- `name`: String (쿠폰명)
- `discountAmount`: Integer (할인 금액)
- `totalQuantity`: Integer (총 발행 가능 수량)
- `expiresAt`: DateTime (만료일시)
- `createdAt`: DateTime (생성일시)

#### CouponIssue (쿠폰 발급 수량)
- `couponId`: Long (PK, FK) - Coupon과 식별 관계
- `issuedQuantity`: Integer (발행된 수량)
- `updatedAt`: DateTime (수정일시)

#### UserCoupon (사용자 쿠폰 - 발행 쿠폰)
- `userCouponId`: Long (PK)
- `userId`: Long
- `couponId`: Long
- `isUsed`: Boolean (사용 여부)
- `usedAt`: DateTime (사용 일시, nullable)
- `issuedAt`: DateTime (발행 일시)

---

## 3. 자원별 설명

### 3.1 상품 도메인

**Item (상품)**
- 시스템의 핵심 판매 상품
- 상품의 기본 정보(이름, 가격 등)를 관리
- 재고는 ItemStock 테이블에서 별도 관리

**ItemStock (상품 재고)**
- 상품별 재고 수량을 관리
- Item과 1:1 식별 관계 (itemId가 PK이자 FK)
- 재고 변동이 잦은 특성상 별도 테이블로 분리하여 동시성 제어 최적화

---

### 3.2 사용자 도메인

**User (사용자)**
- 시스템 사용자 기본 정보
- 포인트는 UserPoint 테이블에서 별도 관리

**UserPoint (사용자 포인트)**
- 사용자별 포인트(적립금) 관리
- User와 1:1 식별 관계 (userId가 PK이자 FK)
- 포인트 충전/사용이 빈번하므로 별도 테이블로 분리하여 동시성 제어 최적화
- 주문 시 결제 수단으로 사용

**UserPointHistory (사용자 포인트 이력)**
- 포인트 충전/사용 이력을 기록
- 모든 포인트 변동 내역 추적 (감사 로그)
- 거래 유형
  - CHARGE: 포인트 충전
  - USE: 포인트 사용 (주문 결제)
- `amount`: 변동 금액 (양수: 충전, 음수: 사용)
- `balanceAfter`: 거래 후 잔액 (데이터 정합성 검증용)
- `orderId`: 주문과 연결

**Cart (장바구니)**
- 사용자별 장바구니

**CartItem (장바구니 항목)**
- 장바구니에 담긴 상품 정보
- 동일 상품의 다른 옵션은 별도 항목으로 관리

---

### 3.3 주문 도메인

**Order (주문)**
- 사용자의 주문 정보
- 주문 상태 관리 (PENDING, PAID, FAILED)
  - PENDING: 주문 생성 (결제 대기)
  - PAID: 결제 완료
  - FAILED: 주문 실패 (재고 부족, 잔액 부족 등)
- 주문 당시의 금액 정보를 스냅샷으로 저장
  - `totalAmount`: 총 상품 금액
  - `couponDiscount`: 쿠폰 할인 금액 (쿠폰 미사용 시 0)
  - `finalAmount`: 최종 결제 금액 (쿠폰 적용 후)

**OrderItem (주문 항목)**
- 주문에 포함된 상품 정보
- 주문 당시의 가격과 상품명 보관 (가격/상품명 변동 대비)
- 장바구니 항목 ID를 통해 어떤 장바구니 항목으로부터 주문되었는지 추적

---

### 3.4 쿠폰 도메인

**Coupon (쿠폰)**
- 시스템에서 발행하는 쿠폰 기본 정보
- 쿠폰명, 할인 금액, 총 발행 가능 수량 등 관리
- 정액 할인형 쿠폰 
- 발행 수량은 CouponIssue 테이블에서 별도 관리

**CouponIssue (쿠폰 발급 수량)**
- 쿠폰별 발급된 수량을 관리
- Coupon과 1:1 식별 관계 (couponId가 PK이자 FK)
- 선착순 쿠폰 발급 시 동시성 제어를 위해 별도 테이블로 분리
- `issuedQuantity`와 `totalQuantity` 비교로 발급 가능 여부 판단

**UserCoupon (사용자 쿠폰)**
- 사용자에게 발행된 쿠폰
- 사용 여부 및 사용 일시 추적
- 한 사용자당 동일 쿠폰 1개만 발행 가능

---
