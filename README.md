# E-Commerce 시스템

## ERD (Entity Relationship Diagram)

![ERD](docs/erd.png)

---

## API 시퀀스 다이어그램

### 1.1 상품 목록 조회 (GET /api/items) - Cursor 기반 페이징

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant Service as ItemService
      participant Repository as ItemRepository

      Controller->>Service: 상품 목록 조회 요청
      alt 첫 페이지
          Service->>Repository: 최신순 조회
      else 다음 페이지
          Service->>Repository: 커서 기반 조회
      end
      Repository-->>Service: 상품 목록 반환
      Service-->>Controller: 상품 목록 응답 반환
```

### 1.2 상품 상세 조회 (GET /api/items/{itemId})

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant Service as ItemService
      participant ItemRepo as ItemRepository
      participant StockRepo as ItemStockRepository

      Controller->>Service: 상품 조회 요청
      Service->>ItemRepo: 상품 정보 조회
      alt 상품 없음
          ItemRepo-->>Service: 상품 없음
          Service-->>Controller: ItemNotFoundException
      else 상품 있음
          ItemRepo-->>Service: 상품 정보 반환
          Service->>StockRepo: 재고 정보 조회
          StockRepo-->>Service: 재고 정보 반환
          Service-->>Controller: 상품 상세 응답
      end
```

### 1.3 인기 상품 조회 (GET /api/items/popular)


```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant Service as ItemService
      participant OrderItemRepo as OrderItemRepository

      Controller->>Service: 인기 상품 조회 요청
      Service->>OrderItemRepo: 최근 3일 판매량 집계 조회
      Note over OrderItemRepo: ORDER_ITEM + ITEM + STOCK JOIN <br/>판매량 기준으로 정렬
      OrderItemRepo-->>Service: 인기 상품 정보 반환
      Note over Service: 순위 정보 조합
      Service-->>Controller: 인기 상품 응답
```

초기에는 MySQL 사용하여 쿼리로만 처리, 후에 별도의 집계 테이블을 두거나 캐싱 사용 고려.

### 1.4 상품 재고 확인 (GET /api/items/{itemId}/stock)

```mermaid
 sequenceDiagram
      participant Controller as ItemController
      participant Service as ItemService
      participant ItemRepo as ItemRepository
      participant StockRepo as ItemStockRepository

      Controller->>Service: 상품 재고 조회 요청
      Service->>ItemRepo: 상품 정보 조회
      alt 상품 없음
          ItemRepo-->>Service: 상품 없음
          Service-->>Controller: ItemNotFoundException
      else 상품 있음
          ItemRepo-->>Service: 상품 정보 반환
          Service->>StockRepo: 재고 정보 조회
          StockRepo-->>Service: 재고 정보 반환
          Service-->>Controller: 재고 조회 응답
      end
```

### 2.1 장바구니 상품 추가 (POST /api/cart/items)

```mermaid
sequenceDiagram
      participant Controller as CartController
      participant Service as CartService
      participant CartRepo as CartRepository
      participant CartItemRepo as CartItemRepository
      participant ItemRepo as ItemRepository
      participant StockRepo as ItemStockRepository

      Controller->>Service: 장바구니 추가 요청
      Note over Service: 🔒 트랜잭션 시작
      Service->>ItemRepo: 상품 정보 조회
      alt 상품 없음
          ItemRepo-->>Service: 상품 없음
          Service-->>Controller: ItemNotFoundException
      else 상품 있음
          ItemRepo-->>Service: 상품 정보 반환
          Service->>StockRepo: 재고 정보 조회
          StockRepo-->>Service: 재고 정보 반환
          alt 재고 부족
              Service-->>Controller: InsufficientStockException
          else 재고 충분
              Service->>CartRepo: 사용자 장바구니 조회
              alt 장바구니 없음
                  CartRepo-->>Service: 장바구니 없음
                  Service->>CartRepo: 새 장바구니 생성
                  CartRepo-->>Service: 장바구니 반환
              else 장바구니 있음
                  CartRepo-->>Service: 장바구니 반환
              end
              Service->>CartItemRepo: 장바구니 항목 저장
              CartItemRepo-->>Service: 장바구니 항목 반환
              Note over Service: ✅ 커밋
              Service-->>Controller: 장바구니 추가 응답
          end
      end
```

### 2.3 장바구니 수량 수정 (PATCH/api/cart/items/{cartItemId})

```mermaid
 sequenceDiagram
      participant Controller as CartController
      participant Service as CartService
      participant CartItemRepo as CartItemRepository
      participant StockRepo as ItemStockRepository

      Controller->>Service: 장바구니 수량 수정 요청
      Note over Service: 🔒 트랜잭션 시작
      Service->>CartItemRepo: 장바구니 항목 조회
      alt 장바구니 항목 없음
          CartItemRepo-->>Service: 항목 없음
          Service-->>Controller: CartItemNotFoundException
      else 장바구니 항목 있음
          CartItemRepo-->>Service: 장바구니 항목 반환
          Service->>StockRepo: 재고 정보 조회
          StockRepo-->>Service: 재고 정보 반환
          alt 재고 부족
              Service-->>Controller: InsufficientStockException
          else 재고 충분
              Note over Service: 수량 업데이트
              Service->>CartItemRepo: 장바구니 항목 저장
              CartItemRepo-->>Service: 장바구니 항목 반환
              Note over Service: ✅ 커밋
              Service-->>Controller: 수량 수정 응답
          end
      end
```


### 3.1 사용자 쿠폰 조회 (GET /api/users/{userId}/coupons)

```mermaid
sequenceDiagram
participant Controller as CouponController
participant Service as CouponService
participant UserCouponRepo as UserCouponRepository
participant CouponRepo as CouponRepository

  Controller->>Service: getUserCoupons(userId)
  Service->>UserCouponRepo: findAllByUserId(userId)
  UserCouponRepo-->>Service: List<UserCoupon>
  Service->>CouponRepo: findAllById(couponIds)
  CouponRepo-->>Service: List<Coupon>
  Note over Service: 쿠폰 정보 
  Service-->>Controller: UserCouponsResponse

```

### 3.2 쿠폰 발급 (POST /api/coupons/{couponId}/issue)

```mermaid
 sequenceDiagram
      participant Controller
      participant CouponService
      participant CouponRepo as CouponRepository
      participant UserCouponRepo as UserCouponRepository

      Controller->>CouponService: 쿠폰 발급 요청

      Note over CouponService: 🔒 트랜잭션 시작

      CouponService->>CouponRepo: 쿠폰 조회
      alt 쿠폰 없음
          CouponRepo-->>CouponService: 쿠폰 없음
          CouponService-->>Controller: CouponNotFoundException
      end

      CouponService->>UserCouponRepo: 중복 발급 확인
      alt 이미 발급받음
          UserCouponRepo-->>CouponService: 발급 이력 존재
          CouponService-->>Controller: CouponAlreadyIssuedException
      end

      CouponService->>CouponRepo: 쿠폰 발급 수량 조회 (FOR UPDATE)
      Note over CouponRepo: 🔒 배타적 락<br/>(동시 발급 제어)

      alt 수량 소진
          CouponService-->>Controller: CouponOutOfStockException
      else 수량 있음
          CouponService->>CouponRepo: 발급 수량 증가
          CouponRepo-->>CouponService: 수량 업데이트 완료
          CouponService->>UserCouponRepo: 사용자 쿠폰 생성
          UserCouponRepo-->>CouponService: 사용자 쿠폰 반환

          Note over CouponService: ✅ 커밋 (락 해제)

          CouponService-->>Controller: 발급 성공 응답
      end
```

### 4.1 주문 생성 (POST /api/orders)

```mermaid
 sequenceDiagram
        participant Controller
        participant OrderFacade
        participant CartService
        participant CouponService
        participant ItemService
        participant PaymentService
        participant OrderService
        participant OrderHistoryService

        Controller->>OrderFacade: 주문 생성 요청

        Note over OrderFacade: 🔒 트랜잭션 시작

        OrderFacade->>CartService: 장바구니 조회
        CartService-->>OrderFacade: 장바구니 항목 반환

        OrderFacade->>CouponService: 쿠폰 검증 (락 없음)
        alt 쿠폰 사용 불가
            CouponService-->>OrderFacade: 쿠폰 사용 불가
            OrderFacade-->>Controller: InvalidCouponException
        end

        OrderFacade->>CouponService: 쿠폰 사용 처리
        CouponService-->>OrderFacade: 쿠폰 사용 완료

        OrderFacade->>ItemService: 재고 확인 및 차감 (FOR UPDATE)
        alt 재고 부족
            ItemService-->>OrderFacade: 재고 부족
            Note over OrderFacade: ❌ 롤백: 쿠폰 사용 취소
            OrderFacade-->>Controller: InsufficientStockException
        end
        ItemService-->>OrderFacade: 재고 차감 완료

        OrderFacade->>OrderService: 주문 생성 (상태: PENDING)
        OrderService-->>OrderFacade: 주문 생성 완료 (PENDING)

        OrderFacade->>PaymentService: 잔액 확인 및 차감 (FOR UPDATE)
        alt 잔액 부족
            PaymentService-->>OrderFacade: 잔액 부족
            OrderFacade->>OrderService: 주문 상태 변경 (FAILED)
            OrderService-->>OrderFacade: 상태 변경 완료
            Note over OrderFacade: ❌ 롤백: 재고 복구, 쿠폰 사용 취소
            OrderFacade-->>Controller: InsufficientBalanceException
        end
        PaymentService-->>OrderFacade: 잔액 차감 완료

        OrderFacade->>OrderService: 주문 상태 변경 (PAID)
        OrderService-->>OrderFacade: 상태 변경 완료

        Note over OrderFacade: ✅ 커밋

        OrderFacade-->>CartService: 🔄 장바구니 삭제 이벤트 발행

        OrderFacade-->>Controller: 주문 생성 응답

        Note over CartService: 🔄 비동기 이벤트 처리
        Note over CartService: 🔒 별도 트랜잭션
        CartService->>CartService: 장바구니 삭제
        Note over CartService: ✅ 커밋

        Note over OrderFacade: 🔄 비동기 이벤트 발행
        OrderFacade-->>OrderHistoryService: 외부 데이터 플랫폼 전송

```
