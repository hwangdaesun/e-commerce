# 인기 상품 조회 쿼리 성능 개선 분석

## 1. 개요

이 문서는 Item 엔티티에서 `salesCount` 칼럼을 제거하고, `ItemView`와 `OrderItem` 테이블을 활용하여 인기 상품을 조회하는 두 가지 방법의 성능을 비교 분석합니다.

### 인기도 계산 공식
```
인기도 점수 = (조회수 × 1) + (판매량 × 10)
```

### 엔티티 관계
- **Item**: 상품 정보 (item_id, name, price, stock)
- **ItemView**: 상품 조회 기록 (item_view_id, item_id, user_id, created_at)
- **Order**: 주문 정보 (order_id, user_id, status, created_at)
- **OrderItem**: 주문 상품 (order_item_id, order_id, item_id, quantity)

---

## 2. 방법 1: 실시간 쿼리 집계 (v1)

### 2.1 설명
매번 요청 시마다 `ItemView`와 `OrderItem` 테이블을 집계하여 인기도를 계산합니다.

### 2.2 장점
- ✅ 실시간 최신 데이터 보장
- ✅ 추가 테이블 불필요
- ✅ 데이터 동기화 이슈 없음

### 2.3 단점
- ❌ 매 요청마다 GROUP BY 집계 쿼리 실행
- ❌ 두 개의 집계 쿼리 + 조인 필요
- ❌ 데이터가 많을수록 성능 저하
- ❌ 애플리케이션 메모리에서 정렬 및 병합 필요

### 2.4 쿼리 및 실행 계획 분석

#### 2.4.1 조회수 집계 쿼리

**쿼리:**
```sql
SELECT
    iv.item_id AS item_id,
    COUNT(*) AS view_count
FROM item_views iv
WHERE iv.created_at > DATE_SUB(NOW(), INTERVAL 3 DAY)
GROUP BY iv.item_id;
```

---

##### 케이스 1: 복합 인덱스 (created_at, item_id) ⭐ **권장**

**인덱스:** `idx_created_at_item_id (created_at DESC, item_id)`

**실행 계획 (JSON 형식):**
```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "iv",
    "type": "range",
    "possible_keys": "idx_created_at_item_id",
    "key": "idx_created_at_item_id",
    "key_len": "8",
    "rows": 895204,
    "filtered": 100,
    "Extra": "Using where; Using index; Using temporary"
  }
]
```

**분석:**
- ✅ **type: range** - created_at 범위 스캔
- ✅ **key: idx_created_at_item_id** - 복합 인덱스 사용
- ✅ **rows: 895,204** - 최근 3일 데이터만 스캔 (전체의 약 1.8%)
- ✅ **Using index** - 커버링 인덱스 (테이블 접근 불필요)
- ⚠️ **Using temporary** - GROUP BY로 인한 임시 테이블 생성 (불가피)
- **성능: 빠름 (100ms~500ms)**

**왜 효율적인가?**
- WHERE 조건 `created_at > ...`이 인덱스 첫 번째 칼럼과 일치
- MySQL이 범위 스캔으로 최근 3일 데이터만 효율적으로 필터링
- 커버링 인덱스로 테이블 접근 없이 처리 완료

---

##### 케이스 2: 복합 인덱스 (item_id, created_at) ❌ **비권장**

**인덱스:** `idx_item_id_created_at (item_id, created_at)`

**실행 계획 (JSON 형식):**
```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "iv",
    "type": "index",
    "possible_keys": "idx_item_id_created_at",
    "key": "idx_item_id_created_at",
    "key_len": "16",
    "rows": 49829871,
    "filtered": 33.33,
    "Extra": "Using where; Using index"
  }
]
```

**분석:**
- ❌ **type: index** - 인덱스 풀 스캔 (전체 인덱스 스캔)
- ❌ **rows: 49,829,871** - 거의 전체 행 스캔 (전체의 99.6%)
- ❌ **filtered: 33.33** - WHERE 조건 필터링 비율 낮음
- ⚠️ **Using index** - 커버링 인덱스이지만 비효율적
- **성능: 느림 (수초) - 인덱스 없는 경우와 유사**

**왜 비효율적인가?**
- WHERE 조건이 `created_at > ...` 인데 인덱스 첫 번째 칼럼이 `item_id`
- MySQL은 인덱스의 첫 번째 칼럼부터 순차적으로 사용
- `item_id` 조건 없이 `created_at` 조건만 있으면 인덱스 효율 급감
- 결과적으로 인덱스 전체를 스캔하게 됨

---

**성능 비교:**

| 인덱스 전략 | type | rows | 성능 | 비고 |
|------------|------|------|------|------|
| **(created_at, item_id)** ⭐ | range | 895,204 | ✅ 빠름 | WHERE 조건 최적화 |
| **(item_id, created_at)** | index | 49,829,871 | ❌ 느림 | 인덱스 칼럼 순서 오류 |

**결론:**
- **케이스 1이 압도적으로 우수** (rows: 895K vs 50M)
- 복합 인덱스는 **WHERE 조건 칼럼을 첫 번째로** 배치해야 함
- 잘못된 칼럼 순서는 인덱스가 없는 것만 못할 수 있음

---

#### 2.4.2 판매량 집계 쿼리

**쿼리:**
```sql
SELECT
    oi.item_id AS item_id,
    SUM(oi.quantity) AS sales_count
FROM order_items oi
INNER JOIN orders o ON oi.order_id = o.order_id
WHERE o.status = 'PAID'
  AND o.created_at > DATE_SUB(NOW(), INTERVAL 3 DAY)
GROUP BY oi.item_id;
```

---

##### 케이스 1: 인덱스 (order_id), (item_id)

**실행 계획 (JSON 형식):**
```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "oi",
    "type": "ALL",
    "possible_keys": null,
    "key": null,
    "rows": 14506301,
    "filtered": 100,
    "Extra": "Using temporary"
  },
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "o",
    "type": "eq_ref",
    "possible_keys": "PRIMARY",
    "key": "PRIMARY",
    "key_len": "8",
    "ref": "ecommerce.oi.order_id",
    "rows": 1,
    "filtered": 11.11,
    "Extra": "Using where"
  }
]
```

**분석:**
- ✅ **oi (OrderItem) 테이블**: type: ALL, rows: 14,506,301 - 풀 스캔이지만 GROUP BY 집계에 필요
- ✅ **o (Order) 테이블**: type: eq_ref, PRIMARY KEY 사용 - 효율적인 조인
- ✅ **filtered: 11.11%** - WHERE 조건 (status, created_at)으로 약 11% 필터링
- ⚠️ **Using temporary** - GROUP BY로 인한 임시 테이블 (불가피)



**왜 효율적인가?**
- OrderItem은 집계가 목적이므로 풀 스캔이 자연스러움
- Order와의 조인은 PRIMARY KEY로 효율적 (eq_ref)
- WHERE 조건 필터링이 적절히 동작 (11.11%)


##### 케이스 2: 복합 인덱스 (created_at, status) 

**실행 계획 (JSON 형식):**

```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "o",
    "partitions": null,
    "type": "range",
    "possible_keys": "PRIMARY,idx_created_at_status",
    "key": "idx_created_at_status",
    "key_len": "8",
    "ref": null,
    "rows": 53478,
    "filtered": 33.33,
    "Extra": "Using where; Using index; Using temporary"
  },
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "oi",
    "partitions": null,
    "type": "ref",
    "possible_keys": "idx_order_items_order_id,idx_order_items_item_id",
    "key": "idx_order_items_order_id",
    "key_len": "8",
    "ref": "ecommerce.o.order_id",
    "rows": 3,
    "filtered": 100,
    "Extra": null
  }
]

```

##### 케이스 3: 복합 인덱스 (status, created_at)

**실행 계획 (JSON 형식):**

```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "o",
    "partitions": null,
    "type": "range",
    "possible_keys": "PRIMARY,idx_status_created_at",
    "key": "idx_status_created_at",
    "key_len": "9",
    "ref": null,
    "rows": 51486,
    "filtered": 100,
    "Extra": "Using where; Using index; Using temporary"
  },
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "oi",
    "partitions": null,
    "type": "ref",
    "possible_keys": "idx_order_items_order_id,idx_order_items_item_id",
    "key": "idx_order_items_order_id",
    "key_len": "8",
    "ref": "ecommerce.o.order_id",
    "rows": 3,
    "filtered": 100,
    "Extra": null
  }
]
```

**분석:**
- ✅ **o (Order) 테이블**: type: range, rows: 51,486 - 인덱스 범위 스캔
- ✅ **key: idx_status_created_at** - (status, created_at) 복합 인덱스 사용
- ✅ **filtered: 100%** - 모든 WHERE 조건이 인덱스에서 완전히 처리됨
- ✅ **key_len: 9** - status (1) + created_at (8) 모두 사용
- ✅ **oi (OrderItem) 테이블**: type: ref - 인덱스를 통한 효율적인 조인
- ✅ **Using index** - 커버링 인덱스 (테이블 접근 불필요)
- ⚠️ **Using temporary** - GROUP BY로 인한 임시 테이블 (불가피)

**왜 효율적인가?**
- WHERE 조건이 `status = 'PAID' AND created_at > ...` 인데 인덱스 순서가 (status, created_at)로 일치
- status가 등호 조건(=)이므로 인덱스 첫 번째 칼럼으로 최적
- 이후 created_at 범위 조건이 연속적으로 적용 가능
- MySQL 옵티마이저가 status로 먼저 필터링한 후 created_at으로 추가 필터링
- 결과적으로 filtered: 100% 달성 (모든 조건이 인덱스에서 처리)
- 드라이빙 테이블이 Order로 변경되어 51K 행만 스캔 후 OrderItem과 조인

---

**성능 비교:**

| 케이스 | 인덱스 전략 | Orders 테이블 | OrderItem 테이블 | 총 예상 rows | 성능 |
|--------|-----------|--------------|-----------------|-------------|------|
| **케이스 1** | (order_id), (item_id) | eq_ref, 1 row | ALL, 14.5M rows | ~14.5M | ❌ 느림 |
| **케이스 2** | (created_at, status) | range, 53K rows (filtered 33.33%) | ref, 3 rows | ~53K × 3 = 159K | ⚠️ 보통 |
| **케이스 3** ⭐ | (status, created_at) | range, 51K rows (filtered 100%) | ref, 3 rows | ~51K × 3 = 153K | ✅ 빠름 |

**상세 비교:**

1. **케이스 1 (기본 인덱스)**
   - OrderItem을 드라이빙 테이블로 사용 (14.5M rows 풀 스캔)
   - Order와 PRIMARY KEY로 효율적 조인 (eq_ref)
   - 하지만 초기 스캔 범위가 너무 큼
   - filtered 11.11%로 대부분의 행이 버려짐

2. **케이스 2 ((created_at, status))**
   - Order를 드라이빙 테이블로 변경 (53K rows)
   - created_at 범위 조건으로 시작
   - **문제점**: status 조건이 두 번째 칼럼이라 완전히 인덱스에서 처리되지 않음
   - filtered 33.33% - status 조건 일부가 WHERE 절에서 후처리됨
   - key_len: 8 (created_at만 사용)

3. **케이스 3 ((status, created_at)) ⭐ 권장**
   - Order를 드라이빙 테이블로 변경 (51K rows)
   - status 등호 조건(=)으로 시작하여 효율적
   - created_at 범위 조건이 연속적으로 적용
   - **filtered 100%** - 모든 조건이 인덱스에서 처리됨
   - key_len: 9 (status + created_at 모두 사용)
   - 가장 적은 rows와 완벽한 필터링

**결론:**
- **케이스 3 ((status, created_at))이 최적**
- 이유:
  1. **등호 조건을 첫 번째 칼럼으로**: status = 'PAID'가 인덱스 선두
  2. **범위 조건을 두 번째 칼럼으로**: created_at > ... 이 그 다음
  3. **filtered 100%**: 모든 WHERE 조건이 인덱스만으로 처리
  4. **최소 rows**: 51,486 (케이스 2보다 약 2,000개 적음)
  5. **드라이빙 테이블 최적화**: OrderItem 14.5M → Order 51K로 대폭 감소

- **복합 인덱스 설계 원칙 재확인**:
  - 등호 조건(=) > 범위 조건(>, <, BETWEEN) 순서로 배치
  - WHERE 절의 모든 조건을 인덱스 순서에 맞춰 구성
  - filtered 100%를 목표로 인덱스 칼럼 순서 결정

---

## 3. 방법 2: 집계 테이블 사용 (v2)

### 3.1 설명
스케줄러가 매 시간마다 `item_popularity_stats` 테이블에 인기도를 미리 계산해두고, 조회 시에는 해당 테이블만 읽습니다.

### 3.2 장점
- ✅ 조회 쿼리가 매우 빠름 (단순 SELECT)
- ✅ 인덱스를 활용한 효율적인 정렬
- ✅ 동시 접속자가 많아도 성능 안정적
- ✅ 스케줄러로 미리 집계하여 실시간 부하 제거
- ✅ 시간별 인기도 이력 추적 가능

### 3.3 단점
- ❌ 데이터가 최대 1시간 지연될 수 있음
- ❌ 추가 테이블 및 스케줄러 필요
- ❌ 스케줄러 실행 시 리소스 사용
- ❌ 집계 테이블의 데이터가 계속 증가 (정기적 정리 필요)

### 3.4 테이블 스키마

```sql
CREATE TABLE item_popularity_stats (
    stats_id BIGINT AUTO_INCREMENT PRIMARY KEY,
    item_id BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    sales_count BIGINT NOT NULL,
    popularity_score BIGINT NOT NULL,
    based_on_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_item_popularity_stats_item_id (item_id),
    INDEX idx_item_popularity_stats_popularity_score (popularity_score DESC)
);
```

**스키마 설계 포인트:**
- `item_id`: 같은 상품에 대해 시간대별 통계를 여러 개저장 
- `based_on_date`: 언제 기준으로 집계한 인기도인지 기록
- `popularity_score`: 인기도 점수 (조회수 × 1 + 판매량 × 10)
- 인덱스: `popularity_score DESC`로 정렬 성능 최적화

---

### 3.5 스케줄러 집계 쿼리

스케줄러가 매 시간 0분에 실행하여 다음 쿼리들을 수행합니다.

#### 3.5.1 조회수 집계 쿼리

```sql
SELECT
    iv.item_id AS item_id,
    COUNT(*) AS view_count
FROM item_views iv
WHERE iv.created_at > DATE_SUB(NOW(), INTERVAL 3 DAY)
GROUP BY iv.item_id;
```

**사용 인덱스:** `idx_item_views_created_at_item_id (created_at, item_id)`

**실행 계획 (JSON 형식):**
```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "iv",
    "type": "range",
    "possible_keys": "idx_item_views_created_at_item_id",
    "key": "idx_item_views_created_at_item_id",
    "key_len": "8",
    "rows": 895204,
    "filtered": 100,
    "Extra": "Using where; Using index; Using temporary"
  }
]
```

**분석:**
- ✅ **type: range** - created_at 범위 스캔으로 최근 3일 데이터만 필터링
- ✅ **key: idx_item_views_created_at_item_id** - 복합 인덱스 사용
- ✅ **rows: 895,204** - 전체의 약 1.8%만 스캔
- ✅ **Using index** - 커버링 인덱스 (테이블 접근 불필요)
- ⚠️ **Using temporary** - GROUP BY로 인한 임시 테이블 (불가피)
- **성능: 100ms~500ms**

**최적화 포인트:**
- 복합 인덱스가 (created_at, item_id) 순서로 구성되어 WHERE 조건과 GROUP BY 모두 최적화
- 커버링 인덱스로 실제 테이블 접근 없이 처리

#### 3.5.2 판매량 집계 쿼리

```sql
SELECT
    oi.item_id AS item_id,
    SUM(oi.quantity) AS sales_count
FROM order_items oi
INNER JOIN orders o ON oi.order_id = o.order_id
WHERE o.status = 'PAID'
  AND o.created_at > DATE_SUB(NOW(), INTERVAL 3 DAY)
GROUP BY oi.item_id;
```

**사용 인덱스:**
- Orders 테이블: `idx_orders_status_created_at (status, created_at)`
- OrderItems 테이블: `idx_order_items_order_id (order_id)`

**실행 계획 (JSON 형식):**
```json
[
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "o",
    "type": "range",
    "possible_keys": "PRIMARY,idx_orders_status_created_at",
    "key": "idx_orders_status_created_at",
    "key_len": "9",
    "rows": 51486,
    "filtered": 100,
    "Extra": "Using where; Using index; Using temporary"
  },
  {
    "id": 1,
    "select_type": "SIMPLE",
    "table": "oi",
    "type": "ref",
    "possible_keys": "idx_order_items_order_id,idx_order_items_item_id",
    "key": "idx_order_items_order_id",
    "key_len": "8",
    "ref": "ecommerce.o.order_id",
    "rows": 3,
    "filtered": 100,
    "Extra": null
  }
]
```

**분석:**
- ✅ **o (Orders) 테이블**: type: range, rows: 51,486 - 복합 인덱스로 효율적 필터링
- ✅ **filtered: 100%** - status와 created_at 조건 모두 인덱스에서 처리
- ✅ **key_len: 9** - status (1) + created_at (8) 모두 사용
- ✅ **oi (OrderItems) 테이블**: type: ref, rows: 3 - 인덱스를 통한 효율적 조인
- ✅ **Using index** - Orders 테이블은 커버링 인덱스
- ⚠️ **Using temporary** - GROUP BY로 인한 임시 테이블 (불가피)
- **총 예상 rows: ~51K × 3 = 153K**
- **성능: 200ms~700ms**

**최적화 포인트:**
- (status, created_at) 복합 인덱스로 등호 조건 → 범위 조건 순서 최적화
- Orders를 드라이빙 테이블로 사용하여 초기 필터링 효율 극대화
- filtered 100% 달성으로 불필요한 행 스캔 제거

#### 3.5.3 집계 결과 INSERT 쿼리

```sql
INSERT INTO item_popularity_stats (
    item_id,
    view_count,
    sales_count,
    popularity_score,
    based_on_date
) VALUES (?, ?, ?, ?, NOW());
```

**인기도 계산 공식:**
```
popularity_score = view_count × 1 + sales_count × 10
```

**스케줄러 실행 주기:** 매 시간 0분 (cron: `0 0 * * * *`)

---

### 3.6 조회 쿼리 및 EXPLAIN 분석

#### 3.6.1 가장 최근 기준 인기 상품 조회 쿼리

```sql
SELECT ips.stats_id,
       ips.item_id,
       ips.view_count,
       ips.sales_count,
       ips.popularity_score,
       ips.based_on_date,
       ips.created_at,
       ips.updated_at
FROM item_popularity_stats ips
WHERE ips.based_on_date = (
    SELECT MAX(ips2.based_on_date)
    FROM item_popularity_stats ips2
)
ORDER BY ips.popularity_score DESC
LIMIT 5;
```

**쿼리 설명:**
- 가장 최근 `based_on_date`를 가진 통계만 필터링 (서브쿼리)
- `popularity_score` 내림차순으로 정렬
- Pageable을 통해 LIMIT 5 적용 (상위 5개 조회)

**실행 계획:**

```json
[
  {
    "id": 1,
    "select_type": "PRIMARY",
    "table": "ips",
    "partitions": null,
    "type": "index",
    "possible_keys": null,
    "key": "idx_item_popularity_stats_popularity_score",
    "key_len": "8",
    "ref": null,
    "rows": 5,
    "filtered": 10,
    "Extra": "Using where"
  },
  {
    "id": 2,
    "select_type": "SUBQUERY",
    "table": "ips2",
    "partitions": null,
    "type": "ALL",
    "possible_keys": null,
    "key": null,
    "key_len": null,
    "ref": null,
    "rows": 174582,
    "filtered": 100,
    "Extra": null
  }
]
```

**분석:**

- 풀 테이블 스캔 수행

### 개선

#### 복합 인덱스 (based_on_date, popularity_score DESC) 추가

```json
[
  {
    "id": 1,
    "select_type": "PRIMARY",
    "table": "ips",
    "partitions": null,
    "type": "index",
    "possible_keys": "idx_based_on_date_popularity,idx_date_score",
    "key": "idx_item_popularity_stats_popularity_score",
    "key_len": "8",
    "ref": null,
    "rows": 10,
    "filtered": 50,
    "Extra": "Using where"
  },
  {
    "id": 2,
    "select_type": "SUBQUERY",
    "table": null,
    "partitions": null,
    "type": null,
    "possible_keys": null,
    "key": null,
    "key_len": null,
    "ref": null,
    "rows": null,
    "filtered": null,
    "Extra": "Select tables optimized away"
  }
]
```

- 서브쿼리가 완벽하게 최적화 되어 수행

---

### 3.7 성능 비교: v1 vs v2

| 항목 | 방법 1 (v1) | 방법 2 (v2) |
|------|------------|------------|
| **조회 응답 시간** | 500ms~2s | 10ms~50ms |
| **스캔 rows** | 15M+ | 1K~10K |
| **인덱스 사용** | 2개 집계 쿼리 | 1개 단순 조회 |
| **실시간성** | ✅ 실시간 | ⚠️ 최대 1시간 지연 |
| **서버 부하** | ❌ 매 요청마다 높음 | ✅ 낮음 (미리 계산) |
| **확장성** | ❌ 트래픽 증가 시 성능 저하 | ✅ 안정적 |
| **추가 리소스** | 없음 | 테이블 + 스케줄러 |

**결론:**
- **트래픽이 많은 서비스**: v2 (집계 테이블) 권장
- **실시간성이 중요한 서비스**: v1 (실시간 집계) 권장
- **하이브리드 전략**: v2를 기본으로 사용하고, v1을 백업/검증용으로 활용
