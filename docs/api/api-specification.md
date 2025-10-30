# API 명세서

## 1. 상품 관리 API

### 1.1 상품 목록 조회

**엔드포인트**
```
GET /api/items
```

**설명**
- 상품 목록을 커서 기반 무한 스크롤 방식으로 조회합니다.
- 재고가 0인 상품도 포함됩니다.
- 최신순(등록일 기준 내림차순)으로 정렬됩니다.

**요청 파라미터 (Query String)**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 | 예시 |
|---------|------|------|------|--------|------|
| cursor | Long | N | 마지막으로 조회한 상품 ID (다음 페이지 조회 시 사용) | null | 100 |
| size | Integer | N | 조회할 상품 수 | 20 | 20 |

**커서 기반 페이징 방식**
- 첫 조회: cursor 없이 요청
- 다음 조회: 응답의 `nextCursor` 값을 cursor 파라미터로 전달
- `hasNext`가 false이면 더 이상 조회할 데이터 없음

**요청 예시**
```http
GET /api/items
GET /api/items?size=20
GET /api/items?cursor=100&size=20
```

**응답 (200 OK)**

```json
{
  "items": [
    {
      "itemId": 1,
      "name": "기본 티셔츠",
      "price": 29000,
      "stock": 0,
      "createdAt": "2025-10-26T10:30:00"
    },
    {
      "itemId": 2,
      "name": "청바지",
      "price": 59000,
      "stock": 50,
      "createdAt": "2025-10-25T14:20:00"
    }
  ],
  "nextCursor": 2,
  "hasNext": true
}
```

**응답 필드 설명**

items 배열:
- `itemId`: 상품 ID
- `name`: 상품명
- `price`: 상품 가격
- `stock`: 재고 수량
- `createdAt`: 상품 등록일시

페이징 정보:
- `nextCursor`: 다음 페이지 조회를 위한 커서 값 (마지막 상품 ID)
- `hasNext`: 다음 페이지 존재 여부

**에러 응답**

**400 Bad Request** - 잘못된 요청 파라미터
```json
{
  "errorCode": "INVALID_PARAMETER",
  "message": "유효하지 않은 정렬 옵션입니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 1.2 상품 상세 조회

**엔드포인트**
```
GET /api/items/{itemId}
```

**설명**
- 특정 상품의 상세 정보를 조회합니다.

**경로 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| itemId | Long | Y | 조회할 상품 ID |

**요청 예시**
```http
GET /api/items/1
```

**응답 (200 OK)**

```json
{
  "itemId": 1,
  "name": "기본 티셔츠",
  "price": 29000,
  "stock": 50,
  "createdAt": "2025-10-26T10:30:00"
}
```

**응답 필드 설명**

- `itemId`: 상품 ID
- `name`: 상품명
- `price`: 상품 가격
- `stock`: 상품 재고 수량
- `createdAt`: 상품 등록일시

**에러 응답**

**404 Not Found** - 존재하지 않는 상품
```json
{
  "errorCode": "ITEM_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 1.3 인기 상품 조회

**엔드포인트**
```
GET /api/items/popular
```

**설명**
- 최근 3일간 판매량 기준 상위 상품을 조회합니다.
- 실시간에 최대한 가깝게 갱신됩니다.

**요청 파라미터 (Query String)**

| 파라미터 | 타입 | 필수 | 설명 | 기본값 | 예시 |
|---------|------|------|------|--------|------|
| limit | Integer | N | 조회할 상품 수 (최대 10) | 5 | 5 |

**요청 예시**
```http
GET /api/items/popular
GET /api/items/popular?limit=3
GET /api/items/popular?limit=10
```

**응답 (200 OK)**

```json
{
  "popularItems": [
    {
      "rank": 1,
      "itemId": 1,
      "itemName": "기본 티셔츠",
      "price": 29000,
      "stock": 50
    },
    {
      "rank": 2,
      "itemId": 2,
      "itemName": "청바지",
      "price": 59000,
      "stock": 30
    },
    {
      "rank": 3,
      "itemId": 3,
      "itemName": "후드티",
      "price": 45000,
      "stock": 20
    }
  ]
}
```

**응답 필드 설명**

popularItems 배열:
- `rank`: 순위 (1부터 시작)
- `itemId`: 상품 ID
- `itemName`: 상품명
- `price`: 상품 가격
- `stock`: 현재 재고 수량

**에러 응답**

**400 Bad Request** - 잘못된 요청 파라미터
```json
{
  "errorCode": "INVALID_PARAMETER",
  "message": "limit은 1 이상 10 이하이어야 합니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 1.4 상품 재고 확인

**엔드포인트**
```
GET /api/items/{itemId}/stock
```

**설명**
- 특정 상품의 실시간 재고를 조회합니다.

**경로 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| itemId | Long | Y | 상품 ID |

**요청 예시**
```http
GET /api/items/1/stock
```

**응답 (200 OK)**

```json
{
  "itemId": 1,
  "itemName": "기본 티셔츠",
  "stock": 50
}
```

**응답 필드 설명**

- `itemId`: 상품 ID
- `itemName`: 상품명
- `stock`: 현재 재고 수량

**에러 응답**

**404 Not Found** - 존재하지 않는 상품
```json
{
  "errorCode": "ITEM_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

## 2. 장바구니 API

### 2.1 장바구니 상품 추가

**엔드포인트**
```
POST /api/cart/items
```

**설명**
- 장바구니에 상품을 추가합니다.
- 상품 추가 시 재고를 확인합니다.

**요청 본문**

```json
{
  "userId": 1,
  "itemId": 1,
  "quantity": 2
}
```

**요청 필드 설명**

- `userId`: 사용자 ID (필수)
- `itemId`: 상품 ID (필수)
- `quantity`: 수량 (필수, 1 이상)

**응답 (201 Created)**

```json
{
  "cartItemId": 1,
  "itemId": 1,
  "itemName": "기본 티셔츠",
  "price": 29000,
  "quantity": 2,
  "totalPrice": 58000,
  "stock": 50,
  "createdAt": "2025-10-26T10:30:00"
}
```

**응답 필드 설명**

- `cartItemId`: 장바구니 항목 ID
- `itemId`: 상품 ID
- `itemName`: 상품명
- `price`: 단가
- `quantity`: 수량
- `totalPrice`: 총 금액 (price * quantity)
- `stock`: 현재 재고
- `createdAt`: 장바구니 추가 일시

**에러 응답**

**400 Bad Request** - 잘못된 요청
```json
{
  "errorCode": "INVALID_QUANTITY",
  "message": "수량은 1 이상이어야 합니다."
}
```

**404 Not Found** - 존재하지 않는 상품
```json
{
  "errorCode": "ITEM_NOT_FOUND",
  "message": "상품을 찾을 수 없습니다."
}
```

**409 Conflict** - 재고 부족
```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "재고가 부족합니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 2.2 장바구니 조회

**엔드포인트**
```
GET /api/cart
```

**설명**
- 현재 사용자의 장바구니 전체 목록을 조회합니다.

**요청 파라미터 (Query String)**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Long | Y | 사용자 ID |

**요청 예시**
```http
GET /api/cart?userId=1
```

**응답 (200 OK)**

```json
{
  "items": [
    {
      "cartItemId": 1,
      "itemId": 1,
      "itemName": "기본 티셔츠",
      "price": 29000,
      "quantity": 2,
      "totalPrice": 58000,
      "stock": 50
    },
    {
      "cartItemId": 2,
      "itemId": 2,
      "itemName": "청바지",
      "price": 59000,
      "quantity": 1,
      "totalPrice": 59000,
      "stock": 30
    }
  ],
  "summary": {
    "totalItems": 2,
    "totalQuantity": 3,
    "totalAmount": 117000
  }
}
```

**응답 필드 설명**

items 배열:
- `cartItemId`: 장바구니 항목 ID
- `itemId`: 상품 ID
- `itemName`: 상품명
- `price`: 단가
- `quantity`: 수량
- `totalPrice`: 항목별 총 금액
- `stock`: 현재 재고

summary 객체:
- `totalItems`: 장바구니 항목 개수
- `totalQuantity`: 총 수량
- `totalAmount`: 총 금액

**에러 응답**

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 2.3 장바구니 수량 수정

**엔드포인트**
```
PATCH /api/cart/items/{cartItemId}
```

**설명**
- 장바구니 항목의 수량을 수정합니다.
- 수량 변경 시 재고를 확인합니다.

**경로 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| cartItemId | Long | Y | 장바구니 항목 ID |

**요청 본문**

```json
{
  "userId": 1,
  "quantity": 5
}
```

**요청 필드 설명**

- `userId`: 사용자 ID (필수)
- `quantity`: 변경할 수량 (필수)

**응답 (200 OK)**

```json
{
  "cartItemId": 1,
  "itemId": 1,
  "itemName": "기본 티셔츠",
  "price": 29000,
  "quantity": 5,
  "totalPrice": 145000,
  "stock": 50
}
```

**에러 응답**

**400 Bad Request** - 잘못된 요청
```json
{
  "errorCode": "INVALID_QUANTITY",
  "message": "수량은 1 이상이어야 합니다."
}
```

**400 Bad Request** - 상품 정보 불일치
```json
{
  "errorCode": "ITEM_MISMATCH",
  "message": "장바구니 항목의 상품 정보가 일치하지 않습니다."
}
```

**404 Not Found** - 존재하지 않는 장바구니 항목
```json
{
  "errorCode": "CART_ITEM_NOT_FOUND",
  "message": "장바구니 항목을 찾을 수 없습니다."
}
```

**409 Conflict** - 재고 부족
```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "재고가 부족합니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

## 3. 쿠폰 API

### 3.1 사용자 쿠폰 조회

**엔드포인트**
```
GET /api/users/{userId}/coupons
```

**설명**
- 현재 사용자가 보유한 쿠폰 목록을 조회합니다.
- 사용 가능한 쿠폰과 사용된 쿠폰을 모두 조회합니다.

**경로 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| userId | Long | Y | 사용자 ID |

**요청 예시**
```http
GET /api/users/1/coupons
```

**응답 (200 OK)**

```json
{
  "coupons": [
    {
      "userCouponId": 1,
      "couponId": 1,
      "couponName": "신규 가입 쿠폰",
      "discountAmount": 5000,
      "isUsed": false,
      "expiresAt": "2025-12-31T23:59:59",
      "issuedAt": "2025-10-26T10:30:00"
    },
    {
      "userCouponId": 2,
      "couponId": 2,
      "couponName": "3월 특별 할인 쿠폰",
      "discountAmount": 10000,
      "isUsed": true,
      "usedAt": "2025-10-25T15:20:00",
      "expiresAt": "2025-11-30T23:59:59",
      "issuedAt": "2025-10-20T12:00:00"
    }
  ]
}
```

**응답 필드 설명**

coupons 배열:
- `userCouponId`: 사용자 쿠폰 ID (발급된 쿠폰의 고유 ID)
- `couponId`: 쿠폰 ID
- `couponName`: 쿠폰명
- `discountAmount`: 할인 금액 (정액)
- `isUsed`: 사용 여부
- `usedAt`: 사용 일시 (사용된 경우에만 존재)
- `expiresAt`: 만료 일시
- `issuedAt`: 발급 일시

**에러 응답**

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

### 3.2 쿠폰 발급

**엔드포인트**
```
POST /api/coupons/{couponId}/issue
```

**설명**
- 특정 쿠폰을 사용자에게 발급합니다.
- 선착순 방식으로 한정된 수량만 발급됩니다.
- 사용자당 동일 쿠폰 1개만 발급 가능합니다.

**경로 파라미터**

| 파라미터 | 타입 | 필수 | 설명 |
|---------|------|------|------|
| couponId | Long | Y | 발급받을 쿠폰 ID |

**요청 본문**

```json
{
  "userId": 1
}
```

**요청 필드 설명**

- `userId`: 사용자 ID (필수)

**응답 (201 Created)**

```json
{
  "userCouponId": 1,
  "couponId": 1,
  "couponName": "신규 가입 쿠폰",
  "discountAmount": 5000,
  "isUsed": false,
  "expiresAt": "2025-12-31T23:59:59",
  "issuedAt": "2025-10-26T10:30:00"
}
```

**응답 필드 설명**

- `userCouponId`: 사용자 쿠폰 ID (발급된 쿠폰의 고유 ID)
- `couponId`: 쿠폰 ID
- `couponName`: 쿠폰명
- `discountAmount`: 할인 금액 (정액)
- `isUsed`: 사용 여부 (항상 false)
- `expiresAt`: 만료 일시
- `issuedAt`: 발급 일시

**에러 응답**

**404 Not Found** - 존재하지 않는 쿠폰
```json
{
  "errorCode": "COUPON_NOT_FOUND",
  "message": "쿠폰을 찾을 수 없습니다."
}
```

**409 Conflict** - 쿠폰 재고 소진
```json
{
  "errorCode": "COUPON_OUT_OF_STOCK",
  "message": "쿠폰이 모두 소진되었습니다."
}
```

**409 Conflict** - 이미 발급받은 쿠폰
```json
{
  "errorCode": "COUPON_ALREADY_ISSUED",
  "message": "이미 발급받은 쿠폰입니다."
}
```

**410 Gone** - 만료된 쿠폰
```json
{
  "errorCode": "COUPON_EXPIRED",
  "message": "만료된 쿠폰입니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---

## 4. 주문 API

### 4.1 주문 생성

**엔드포인트**
```
POST /api/orders
```

**설명**
- 장바구니의 상품들로 주문을 생성합니다.
- 주문 프로세스:
  1. 장바구니 상품 조회
  2. 재고 최종 확인
  3. 쿠폰 할인 적용 (선택적)
  4. 잔액 차감 (결제)
  5. 재고 차감
  6. 주문 완료
- 재고 부족 또는 잔액 부족 시 전체 주문이 취소됩니다.

**요청 본문**

```json
{
  "userId": 1,
  "cartItemIds": [1, 2, 3],
  "userCouponId": 1
}
```

**요청 필드 설명**

- `userId`: 사용자 ID (필수)
- `cartItemIds`: 주문할 장바구니 항목 ID 배열 (필수)
- `userCouponId`: 사용할 쿠폰 ID (선택, null 가능)

**응답 (201 Created)**

```json
{
  "orderId": 1,
  "orderItems": [
    {
      "orderItemId": 1,
      "itemId": 1,
      "itemName": "기본 티셔츠",
      "price": 29000,
      "quantity": 2,
      "totalPrice": 58000
    },
    {
      "orderItemId": 2,
      "itemId": 2,
      "itemName": "청바지",
      "price": 59000,
      "quantity": 1,
      "totalPrice": 59000
    }
  ],
  "totalAmount": 117000,
  "couponDiscount": 5000,
  "finalAmount": 112000,
  "paymentAmount": 112000,
  "couponUsed": {
    "userCouponId": 1,
    "couponName": "신규 가입 쿠폰",
    "discountAmount": 5000
  },
  "orderedAt": "2025-10-26T10:30:00"
}
```

**응답 필드 설명**

- `orderId`: 주문 ID
- `orderItems`: 주문 항목 배열
  - `orderItemId`: 주문 항목 ID
  - `itemId`: 상품 ID
  - `itemName`: 상품명
  - `price`: 단가
  - `quantity`: 수량
  - `totalPrice`: 항목별 총 금액
- `totalAmount`: 총 주문 금액 (할인 전)
- `couponDiscount`: 쿠폰 할인 금액
- `finalAmount`: 최종 결제 금액 (할인 후)
- `paymentAmount`: 실제 결제된 금액
- `couponUsed`: 사용된 쿠폰 정보 (쿠폰 미사용 시 null)
  - `userCouponId`: 사용자 쿠폰 ID
  - `couponName`: 쿠폰명
  - `discountAmount`: 할인 금액
- `orderedAt`: 주문 일시

**에러 응답**

**400 Bad Request** - 잘못된 요청
```json
{
  "errorCode": "INVALID_CART_ITEMS",
  "message": "장바구니 항목이 비어있습니다."
}
```

**400 Bad Request** - 쿠폰 적용 불가
```json
{
  "errorCode": "INVALID_COUPON",
  "message": "사용할 수 없는 쿠폰입니다."
}
```

**404 Not Found** - 장바구니 항목 없음
```json
{
  "errorCode": "CART_ITEM_NOT_FOUND",
  "message": "장바구니 항목을 찾을 수 없습니다."
}
```

**409 Conflict** - 재고 부족
```json
{
  "errorCode": "INSUFFICIENT_STOCK",
  "message": "재고가 부족합니다."
}
```

**409 Conflict** - 잔액 부족
```json
{
  "errorCode": "INSUFFICIENT_BALANCE",
  "message": "잔액이 부족합니다."
}
```

**500 Internal Server Error** - 서버 오류
```json
{
  "errorCode": "INTERNAL_SERVER_ERROR",
  "message": "서버 내부 오류가 발생했습니다."
}
```

---
