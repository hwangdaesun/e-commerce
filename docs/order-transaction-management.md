# 주문 프로세스의 트랜잭션 관리 및 동시성 제어

## 1. 트랜잭션 범위 축소

### 문제: 불필요하게 긴 트랜잭션

```java
// ❌ 기존: 모든 작업이 하나의 트랜잭션
@Transactional
public CreateOrderResponse create(...) {
    validateCartItems();      // 읽기 - 트랜잭션 불필요
    validateItems();          // 읽기 - 트랜잭션 불필요
    useCoupon();              // 쓰기
    createOrder();            // 쓰기
    decreaseStock();          // 쓰기
    usePoint();               // 쓰기
    deleteCartItemsAsync();   // 비동기 - 트랜잭션 불필요
}
// 문제: 커넥션 오래 점유, 불필요한 락 유지
```

### 해결: 핵심 로직만 트랜잭션으로 보호

```java
// ✅ 개선: 역할 분리
public CreateOrderResponse create(...) {
    // 1. 검증 (트랜잭션 밖)
    List<CartItem> cartItems = validateCartItems();
    List<Item> items = validateItems();

    // 2. 쿠폰 사용 (독립 트랜잭션)
    Coupon coupon = couponService.useCoupon(userCouponId);

    // 3. 핵심 주문 처리 (트랜잭션)
    OrderCreateResult result = orderTransactionService
        .executeCoreOrderTransaction(...);

    // 4. 비동기 후처리 (트랜잭션 밖)
    deleteCartItemsAsync();
}
```

---

## 2. 비관적 락의 범위 축소

### 문제: 과도한 락 범위

```java
// ❌ 비효율: 재고에 락을 걸고 모든 작업 수행
@Transactional
public void createOrder(...) {
    Stock stock = stockRepository.findByIdWithLock(itemId); // 락 획득

    createOrder();        // 락 유지 중...
    stock.decrease();     // 재고 차감
    usePoint();           // 락 유지 중...
    // 락을 오래 유지 → 다른 요청 대기
}
```

### 해결: 독립 트랜잭션으로 락 범위 최소화

```java
// ✅ 개선: 필요한 시점에만 락 획득
@Transactional
public OrderCreateResult execute(...) {
    createOrder();                    // 락 없음

    itemStockService.decreaseStock(); // 재고 락 획득 → 즉시 커밋 (락 해제)
    userPointService.use();           // 포인트 락 획득 → 즉시 커밋 (락 해제)

    completeOrder();                  // 락 없음
}
```

```java
// ItemStockService
@Transactional(propagation = Propagation.REQUIRES_NEW)
public void decreaseStock(...) {
    Stock stock = stockRepository.findByIdWithLock(itemId); // 락 획득
    stock.decrease(quantity);
    // 커밋 → 락 해제
}
```

---

## 3. 부모/자식 트랜잭션과 예외 전파

### REQUIRES_NEW의 특징

- 자식 트랜잭션은 **즉시 커밋** → 데이터베이스 반영됨
- 자식 실패 시 **예외를 부모로 전파** → 부모도 롤백
- **문제**: 자식이 커밋된 후 다른 자식이 실패하면?

### 시나리오 1: 재고 차감 실패

```java
@Transactional  // 부모 트랜잭션
public OrderCreateResult execute(...) {
    createOrder();              // ✅ 성공

    decreaseStock();            // ❌ 실패 (InsufficientStockException)
    // → 부모 트랜잭션 롤백 → 주문도 롤백 ✅
}
```

**결과:**
- ❌ 재고 차감 실패 → 자식 롤백
- ❌ 예외 전파 → 부모 롤백 (주문도 롤백)
- ✅ 정합성 유지

### 시나리오 2: 포인트 차감 실패 (⚠️ 문제 발생)

```java
@Transactional  // 부모 트랜잭션
public OrderCreateResult execute(...) {
    createOrder();              // ✅ 성공

    decreaseStock();            // ✅ 성공 → 즉시 커밋 (DB 반영)

    usePoint();                 // ❌ 실패 (InsufficientPointException)
    // → 부모 롤백 → 주문 롤백
    // ⚠️ 문제: 재고는 이미 차감됨 (롤백 불가)
}
```

**결과:**
- ✅ 재고 차감 성공 → 자식 커밋 → **이미 DB 반영됨**
- ❌ 포인트 차감 실패 → 자식 롤백
- ❌ 예외 전파 → 부모 롤백 (주문 롤백)
- ⚠️ **데이터 불일치**: 재고만 차감된 상태

---

## 4. 보상 트랜잭션 (Compensating Transaction)

### 문제 해결: 이미 커밋된 데이터 복구

```java
@Transactional
public OrderCreateResult execute(...) {
    // 1. 주문 생성
    OrderCreateResult result = createOrder();

    // 2. 재고 차감
    try {
        decreaseStock();  // ✅ 커밋됨
    } catch (Exception e) {
        // 보상: 쿠폰 복구
        rollbackCoupon();
        throw e;
    }

    // 3. 포인트 차감
    try {
        usePoint();  // ❌ 실패
    } catch (Exception e) {
        // ⚠️ 재고는 이미 커밋됨
        // 보상: 재고 복구 + 쿠폰 복구
        rollbackStock();   // ⏪ 재고 복구
        rollbackCoupon();  // ⏪ 쿠폰 복구
        throw e;
    }

    return result;
}
```

### 보상 트랜잭션 구현

```java
@Component
public class OrderRollbackHandler {

    // 재고 복구
    public void rollbackItemStock(List<CartItem> cartItems, List<Item> items) {
        itemStockService.increaseStock(cartItems, items);  // 재고 원복
    }

    // 재고 차감 실패 시: 쿠폰만 복구
    public void rollbackForStockFailure(Long userCouponId) {
        cancelCoupon(userCouponId);
    }

    // 포인트 차감 실패 시: 재고 + 쿠폰 복구
    public void rollbackForPaymentFailure(Long userCouponId,
                                          List<CartItem> cartItems,
                                          List<Item> items) {
        rollbackItemStock(cartItems, items);  // 재고 복구
        cancelCoupon(userCouponId);           // 쿠폰 복구
    }
}
```

---

## 5. 최종 트랜잭션 구조

```
[OrderCreateUseCase - 트랜잭션 없음]
│
├─ 검증 (트랜잭션 밖)
│
├─ 쿠폰 사용 (독립 TX)
│
├─ [OrderTransactionService - @Transactional]
│   │
│   ├─ 주문 생성 (부모 TX)
│   │
│   ├─ 재고 차감 (자식 TX: REQUIRES_NEW + 비관적 락)
│   │   └─ 실패 시: rollbackForStockFailure()
│   │
│   ├─ 포인트 차감 (자식 TX: REQUIRES_NEW + 비관적 락)
│   │   └─ 실패 시: rollbackForPaymentFailure()
│   │
│   └─ 주문 완료 (부모 TX)
│
└─ 비동기 후처리 (트랜잭션 밖)
```