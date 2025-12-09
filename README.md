# E-Commerce 시스템


## ERD (Entity Relationship Diagram)

![ERD](docs/erd.png)

---

## API 시퀀스 다이어그램

### 1.1 상품 목록 조회 (GET /api/items) - Cursor 기반 페이징

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant UseCase as ItemViewUseCase
      participant Repository as ItemRepository

      Controller->>UseCase: 상품 목록 조회 요청
      alt 첫 페이지
          UseCase->>Repository: 최신순 조회
      else 다음 페이지
          UseCase->>Repository: 커서 기반 조회
      end
      Repository-->>UseCase: 상품 목록 반환
      UseCase-->>Controller: 상품 목록 응답 반환
```

### 1.2 상품 상세 조회 (GET /api/items/{itemId})

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant UseCase as ItemViewUseCase
      participant Validator as ItemValidator
      participant Repository as ItemRepository

      Controller->>UseCase: 상품 조회 요청
      UseCase->>Validator: 상품 존재 여부 검증
      Validator->>Repository: 상품 정보 조회
      alt 상품 없음
          Repository-->>Validator: 상품 없음
          Validator-->>UseCase: ItemNotFoundException
          UseCase-->>Controller: ItemNotFoundException
      else 상품 있음
          Repository-->>Validator: 상품 정보 반환 (재고 포함)
          Validator-->>UseCase: 검증 완료
          UseCase-->>Controller: 상품 상세 응답
      end
```

### 1.3 인기 상품 조회 (GET /api/items/popular)

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant UseCase as ItemViewUseCase
      participant PopularityService as ItemPopularityService
      participant ItemRepo as ItemRepository
      participant ItemViewRepo as ItemViewRepository

      Controller->>UseCase: 인기 상품 조회 요청
      UseCase->>ItemRepo: 최근 3일 판매 상품 조회
      ItemRepo-->>UseCase: 판매 상품 목록 반환
      UseCase->>ItemViewRepo: 상품별 조회수 집계
      ItemViewRepo-->>UseCase: 조회수 정보 반환
      UseCase->>PopularityService: 인기도 계산 (조회수×9 + 판매량×1)
      PopularityService-->>UseCase: 인기도 순위 목록
      UseCase->>ItemRepo: 인기 상품 정보 조회
      ItemRepo-->>UseCase: 상품 상세 정보
      UseCase-->>Controller: 인기 상품 응답
```

인기도 계산식: `조회수 × 9 + 판매량 × 1` 가중치 적용

### 1.4 상품 재고 확인 (GET /api/items/{itemId}/stock)

```mermaid
sequenceDiagram
      participant Controller as ItemController
      participant UseCase as ItemViewUseCase
      participant Validator as ItemValidator
      participant Repository as ItemRepository

      Controller->>UseCase: 상품 재고 조회 요청
      UseCase->>Validator: 상품 존재 여부 검증
      Validator->>Repository: 상품 정보 조회
      alt 상품 없음
          Repository-->>Validator: 상품 없음
          Validator-->>UseCase: ItemNotFoundException
          UseCase-->>Controller: ItemNotFoundException
      else 상품 있음
          Repository-->>Validator: 상품 정보 반환 (재고 포함)
          Validator-->>UseCase: 검증 완료
          UseCase-->>Controller: 재고 조회 응답
      end
```

Item 엔티티에 재고 정보가 포함되어 있어 실시간 재고 확인 가능.

### 2.1 장바구니 상품 추가 (POST /api/cart/items)

```mermaid
sequenceDiagram
      participant Controller as CartController
      participant UseCase as CartAddUseCase
      participant ItemRepo as ItemRepository
      participant CartRepo as CartRepository
      participant CartItemRepo as CartItemRepository

      Controller->>UseCase: 장바구니 추가 요청
      UseCase->>ItemRepo: 상품 조회 및 재고 확인
      alt 상품 없음 또는 재고 부족
          ItemRepo-->>UseCase: Exception
          UseCase-->>Controller: Exception
      else 재고 충분
          UseCase->>CartRepo: 장바구니 조회/생성
          CartRepo-->>UseCase: 장바구니 반환
          UseCase->>CartItemRepo: 장바구니 항목 저장
          CartItemRepo-->>UseCase: 저장 완료
          UseCase-->>Controller: 장바구니 추가 응답
      end
```

### 2.3 장바구니 수량 수정 (PATCH/api/cart/items/{cartItemId})

```mermaid
sequenceDiagram
      participant Controller as CartController
      participant UseCase as CartUpdateUseCase
      participant CartItemRepo as CartItemRepository
      participant ItemRepo as ItemRepository

      Controller->>UseCase: 장바구니 수량 수정 요청
      UseCase->>CartItemRepo: 장바구니 항목 조회 및 소유권 검증
      alt 항목 없음 또는 권한 없음
          CartItemRepo-->>UseCase: Exception
          UseCase-->>Controller: Exception
      else 검증 완료
          UseCase->>ItemRepo: 상품 조회 및 재고 확인
          alt 재고 부족
              ItemRepo-->>UseCase: InsufficientStockException
              UseCase-->>Controller: InsufficientStockException
          else 재고 충분
              UseCase->>CartItemRepo: 수량 업데이트 및 저장
              CartItemRepo-->>UseCase: 저장 완료
              UseCase-->>Controller: 수량 수정 응답
          end
      end
```


### 3.1 사용자 쿠폰 조회 (GET /api/users/{userId}/coupons)

```mermaid
sequenceDiagram
      participant Controller as CouponController
      participant UseCase as CouponViewUseCase
      participant UserCouponRepo as UserCouponRepository
      participant CouponRepo as CouponRepository

      Controller->>UseCase: 사용자 쿠폰 조회 요청
      UseCase->>UserCouponRepo: 사용자 쿠폰 목록 조회
      UserCouponRepo-->>UseCase: 사용자 쿠폰 목록
      UseCase->>CouponRepo: 쿠폰 정보 조회 (각 쿠폰별)
      CouponRepo-->>UseCase: 쿠폰 정보
      UseCase-->>Controller: 쿠폰 목록 응답
```

### 3.2 쿠폰 발급 (POST /api/coupons/{couponId}/issue)

```mermaid
sequenceDiagram
      participant Controller as CouponController
      participant UseCase as CouponIssueUseCase
      participant LockService as CouponIssueLockService
      participant CouponRepo as CouponRepository
      participant CouponStockRepo as CouponStockRepository
      participant UserCouponRepo as UserCouponRepository

      Controller->>UseCase: 쿠폰 발급 요청
      UseCase->>LockService: 쿠폰 발급 처리 (@PessimisticLock)
      Note over LockService: 🔒 ReentrantLock (3초 timeout)
      LockService->>CouponRepo: 쿠폰 조회
      LockService->>UserCouponRepo: 중복 발급 확인
      LockService->>CouponStockRepo: 쿠폰 재고 조회 및 차감
      alt 실패 (쿠폰 없음/중복/재고 부족)
          LockService-->>UseCase: Exception
          Note over LockService: 🔓 Lock 해제
          UseCase-->>Controller: Exception
      else 성공
          LockService->>UserCouponRepo: 사용자 쿠폰 생성
          UserCouponRepo-->>LockService: 쿠폰 발급 완료
          Note over LockService: 🔓 Lock 해제
          LockService-->>UseCase: 발급 성공
          UseCase-->>Controller: 발급 성공 응답
      end
```

### 4.1 주문 생성 (POST /api/orders)

```mermaid
sequenceDiagram
        participant Controller as OrderController
        participant UseCase as OrderCreateUseCase
        participant CouponService
        participant ItemStockService
        participant OrderService
        participant PaymentService
        participant RollbackHandler as OrderRollbackHandler
        participant CartItemService
        participant ExternalDataPlatform as ExternalDataPlatformService

        Controller->>UseCase: 주문 생성 요청
        UseCase->>UseCase: 장바구니 및 상품 검증

        UseCase->>CouponService: 쿠폰 사용 처리
        alt 쿠폰 사용 실패
            CouponService-->>UseCase: Exception
            UseCase-->>Controller: Exception
        end

        UseCase->>ItemStockService: 재고 차감 (@OptimisticLock)
        Note over ItemStockService: 🔒 CAS 연산 (최대 5회 재시도)
        alt 재고 차감 실패
            ItemStockService-->>UseCase: Exception
            UseCase->>RollbackHandler: 쿠폰 롤백
            RollbackHandler-->>UseCase: 롤백 완료
            UseCase-->>Controller: Exception
        end

        UseCase->>OrderService: 주문 생성 (상태: PENDING)
        alt 주문 생성 실패
            OrderService-->>UseCase: Exception
            UseCase->>RollbackHandler: 쿠폰 + 재고 롤백
            RollbackHandler-->>UseCase: 롤백 완료
            UseCase-->>Controller: Exception
        end

        UseCase->>PaymentService: 포인트 차감 (@PessimisticLock)
        Note over PaymentService: 🔒 ReentrantLock (3초 timeout)
        alt 포인트 부족
            PaymentService-->>UseCase: Exception
            UseCase->>RollbackHandler: 쿠폰 + 재고 롤백
            RollbackHandler-->>UseCase: 롤백 완료
            UseCase-->>Controller: Exception
        end

        UseCase->>OrderService: 주문 상태 변경 (PAID)
        OrderService-->>UseCase: 주문 완료

        UseCase->>CartItemService: 🔄 장바구니 삭제 (@Async)
        Note over CartItemService: 비동기 처리

        UseCase->>ExternalDataPlatform: 🔄 외부 데이터 플랫폼 전송 (@Async)
        Note over ExternalDataPlatform: 비동기 처리

        UseCase-->>Controller: 주문 생성 응답
```
