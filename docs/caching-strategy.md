# 인기 상품 조회 캐싱 전략

## 개요

Redis 캐싱을 활용하여 인기 상품 조회 API의 성능을 개선합니다.
`@Cacheable` 어노테이션을 사용해 ItemDto 자체를 캐싱하며, 하루 주기로 자동 갱신됩니다.

---

## 구현 아키텍처

### 1. 캐시 설정
```java
// RedisCacheConfig.java
- popular-items: TTL 1일 (인기 상품 전체 목록)
- item: TTL 1시간 (개별 인기 상품 상세)
```

### 2. 구현 방식

#### 시나리오 1: 인기 상품 전체 목록 조회
```java
@Cacheable(value = "popular-items", key = "#limit")
@Transactional(readOnly = true)
public PopularItemsDto getPopularItemsV1(int limit) {
    // 최근 3일 판매량/조회수 기반 인기도 계산
    // PopularItemsDto (List<PopularItemDto>) 반환
}
```

**Redis 저장 구조:**
```
Key: popular-items::5  → Value: PopularItemsDto { items: [ItemDto1, ItemDto2, ...] }
Key: popular-items::10 → Value: PopularItemsDto { items: [ItemDto1, ItemDto2, ...] }
```

#### 시나리오 2: 인기 상품 상세 조회
```java
@Cacheable(value = "item", key = "#itemId")
@Transactional(readOnly = true)
public ItemDto getItemV1(Long itemId) {
    // 개별 상품 정보를 ItemDto로 반환
}
```

**Redis 저장 구조:**
```
Key: item::1 → Value: ItemDto { itemId: 1, name: "상품1", price: 10000, stock: 50 }
Key: item::5 → Value: ItemDto { itemId: 5, name: "상품5", price: 20000, stock: 30 }
```

---

## 장점

### 1. 구현 간단성
- `@Cacheable` 어노테이션 한 줄로 캐싱 완료
- Spring Cache Abstraction이 직렬화/역직렬화 자동 처리
- 추가 코드 최소화, 유지보수 용이

### 2. API 조회 속도 대폭 개선
| 조회 유형 | 캐싱 전 | 캐싱 후 | 개선율 |
|---------|--------|--------|-------|
| 인기 상품 전체 목록 | 100~200ms | 5~10ms | **10~20배** |
| 인기 상품 상세 조회 | 20~30ms | 3~5ms | **5~7배** |

### 3. DB 부하 감소
- 복잡한 집계 쿼리 (조회수/판매량) 실행 빈도 감소
- Cache Hit 시 DB 접근 0회
- 인기 상품에 트래픽 집중 시 효과적

### 4. 완전한 데이터 캐싱
- ItemDto 전체를 캐싱하여 추가 DB 조회 불필요
- 인기도 계산 결과까지 포함

### 5. 자동 TTL 관리
- 하루(인기 목록)/1시간(상세) 주기로 자동 갱신
- 별도 캐시 갱신 로직 불필요

---

## 단점

### 1. Redis 메모리 중복 사용
- 같은 ItemDto가 여러 캐시 키에 중복 저장
- 예상 메모리 사용량:
  ```
  # 시나리오 1: 인기 상품 목록 (limit별)
  - limit=5: 10KB
  - limit=10: 20KB
  → 총 30KB (상품 데이터 중복)

  # 시나리오 2: 개별 상품 상세
  - 인기 상품 20개: 40KB

  # 전체 메모리: 약 70KB (미미한 수준)
  ```

### 2. 캐시 정합성 문제
- **재고 변동**: 재고 감소 시 캐시와 DB 불일치
  - 최대 1시간(상세)/1일(목록) 지연 가능
  - 재고 부족 시 주문 단계에서 검증하므로 치명적이지 않음
- **가격 변동**: 가격 변경 시 즉시 반영 안 됨
  - 민감한 정보라면 `@CacheEvict`로 수동 무효화 필요

### 3. TTL 의존성
- 캐시 만료 전까지 오래된 데이터 제공
- 실시간 업데이트 불가능
- 트레이드오프: 성능 vs 실시간성

### 4. 캐시 무효화 복잡성
- limit별로 별도 캐시 생성 (시나리오 1)
- 상품 정보 변경 시 여러 캐시 키 무효화 필요
- 현재는 TTL 기반으로만 갱신

### 5. 첫 조회 성능 (Cache Miss)
- 첫 요청 시 DB 조회 후 캐싱
- 캐시 워밍업 미구현 시 초기 지연 발생

---

## 적합성 판단

### ✅ 이 전략이 적합한 경우

1. **조회 빈도 >> 변경 빈도**
   - 인기 상품은 조회가 많고 변경은 적음
   - 읽기 성능이 중요한 경우

2. **실시간성이 크게 중요하지 않음**
   - 최대 1시간~1일 데이터 지연 허용 가능
   - 인기 상품 순위는 하루 단위로 변경되어도 무방

3. **구현 간단성 중요**
   - 빠른 개발 및 배포가 필요
   - 유지보수 비용 최소화

4. **메모리 사용량이 크지 않음**
   - 인기 상품 수가 제한적 (100개 이하)
   - limit 값이 제한적 (5, 10 정도)

5. **트래픽이 특정 상품에 집중**
   - 인기 상품에 반복 조회가 많은 경우
   - Cache Hit Rate가 높을 것으로 예상

### ❌ 이 전략이 부적합한 경우

1. **실시간 재고 반영 필수**
   - 재고가 실시간으로 정확해야 하는 경우
   - 캐시 대신 DB 직접 조회 필요

2. **가격 변동이 빈번**
   - 동적 가격 책정 시스템
   - 실시간 할인/이벤트 적용

3. **limit 값이 매우 다양**
   - limit 1~100까지 다양하게 요청되는 경우
   - Sorted Set 방식이 메모리 효율적

4. **완벽한 실시간 인기도 반영 필요**
   - 조회/주문 시 즉시 순위 반영
   - Sorted Set + score 업데이트 방식 고려