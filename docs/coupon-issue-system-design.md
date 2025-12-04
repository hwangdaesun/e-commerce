# 선착순 쿠폰 발급 시스템 설계서

## 1. 개요 (Overview)

### 1.1 목적
대규모 트래픽 상황에서 선착순 쿠폰 발급의 **순서 보장(Fairness)** 및 **DB 부하 격리(Backpressure)**를 달성합니다.

### 1.2 기존 문제점
- **분산락 방식의 한계**:
  - 락 획득 경쟁(Race Condition)으로 인한 순서 역전 가능성
  - DB에 직접적인 트래픽 전달로 부하 발생

### 1.3 개선 방향
**Producer-Consumer 패턴**을 도입하여:
- 요청을 Redis Queue에 **직렬화하여 저장**
- MySQL이 **처리 가능한 속도**로 소비(Consume)하여 처리

**개선 효과**:
- ✅ 선착순 보장 (요청 순서대로 Queue 적재)
- ✅ 부하 조절 가능 (Consumer가 처리 속도 제어)
- ✅ 동시성 문제 원천 차단 (Queue 기반 직렬화)

---

## 2. 핵심 아키텍처 (Architecture)

시스템은 크게 **요청 적재(Enqueue)**와 **요청 처리(Dequeue & Process)** 두 단계로 구성됩니다.

### 2.1 기술 스택 선정: Redis Stream vs List

| 비교 항목 | Redis List (RPUSH/LPOP) | Redis Stream (XADD/XREADGROUP) |
|----------|------------------------|-------------------------------|
| **순서 보장** | O (FIFO) | O (Time-based ID) |
| **데이터 안정성** | **낮음** (Pop 하는 순간 큐에서 사라짐.<br>처리 중 서버 죽으면 유실) | **높음** (ACK를 보내기 전까지<br>Pending 상태로 보존. 장애 복구 가능) |
| **그룹 소비** | 구현 복잡 | **Consumer Group** 기본 지원 |
| **멀티 인스턴스** | 수동 구현 필요 | Consumer Group으로 자동 로드밸런싱 |
| **재처리** | 불가능 (메시지 유실) | Pending 메시지 재처리 가능 |
| **추천** | 간단한 작업 | **결제/쿠폰 등 중요 로직** |

**선택: Redis Stream**
- 쿠폰 발급은 금전적 가치가 있는 중요한 작업
- 메시지 유실 방지 및 장애 복구 필요
- Consumer Group을 통한 멀티 인스턴스 지원

---

## 3. 상세 프로세스 (Detailed Flow)

### Phase 1. 요청 적재 (Producer: API Server)

사용자 요청을 받아 대기열에 넣는 단계입니다. **DB 부하가 전혀 발생하지 않습니다.**

#### 3.1.1 진입 제어 (Traffic Control)
```
목적: 무한정 Queue에 넣으면 Redis 메모리 초과 가능
방법: Redis 재고 확인 (참조용)
```

**구현**:
- Redis에서 쿠폰별 남은 재고 확인 (`GET coupon:stock:{couponId}`)
- **(실제 재고 × 2)** 까지만 Queue 진입 허용
  - 중복 요청, 처리 실패 등을 고려한 버퍼
  - 재고를 초과한 요청은 즉시 거부

**중요**:
- ⚠️ **Producer는 Redis 재고를 참조만 함** (차감하지 않음)
- ✅ **실제 재고 차감은 Consumer에서 MySQL 처리 후 수행**
- Redis 재고는 **필터링 용도**로만 사용 (Source of Truth는 MySQL)

**코드 위치**: `CouponRedisStockService.java`

#### 3.1.2 중복 요청 필터링
```
목적: 동일 사용자의 중복 진입 방지
방법: Redis Set 확인 (참조용)
```

**구현**:
- Redis Set에서 사용자 발급 이력 확인 (`SISMEMBER coupon:issued:users:{couponId} {userId}`)
- 이미 발급받은 사용자는 Queue 진입 거부

**중요**:
- ⚠️ **Producer는 Redis Set을 참조만 함** (추가하지 않음)
- ✅ **실제 Set 추가는 Consumer에서 MySQL 처리 후 수행**
- Redis Set은 **필터링 용도**로만 사용 (Source of Truth는 MySQL)

**코드 위치**: `CouponRedisStockService.java`

#### 3.1.3 Queue 적재
```
자료구조: Redis Stream
명령어: XADD
```

**메시지 구조**:
```json
{
  "userId": "123",
  "couponId": "456",
  "requestTime": "2025-12-04T10:30:00"
}
```

**코드 위치**: `CouponIssueQueueProducer.java`

#### 3.1.4 사용자 응답
```
"대기열에 진입했습니다. 잠시만 기다려주세요."
```

실제 발급 결과는 별도 API로 조회하거나 WebSocket/Polling으로 확인 가능합니다.

---

### Phase 2. 요청 처리 (Consumer: Worker Server)

줄 서 있는 요청을 하나씩 꺼내어 실제 DB에 반영하는 단계입니다.

#### 3.2.1 메시지 읽기
```
명령어: XREADGROUP
Consumer Group: coupon-issue-group
Batch Size: 100개씩
```

**Consumer Group 전략**:
- **모든 인스턴스가 동일한 Consumer Group 사용**
  - 같은 메시지가 여러 인스턴스에 중복 전달되지 않음
  - Redis Stream이 자동으로 메시지를 분산 배포
  - 멀티 인스턴스 환경에서 로드 밸런싱 자동 수행

**Consumer Name 전략**:
- **인스턴스별 고정된 Consumer Name 사용**
  - 형식: `coupon-consumer-{pid}@{hostname}`
  - 예시: `coupon-consumer-12345@server-01`
  - 각 인스턴스가 고유한 Consumer Name으로 메시지 처리
  - 실패한 메시지(Pending)를 동일 Consumer가 재처리 가능

**로드 밸런싱 원리**:
```
[Instance A - Consumer: coupon-consumer-1@server-a] ─┐
[Instance B - Consumer: coupon-consumer-2@server-b] ─┼─ Consumer Group: "coupon-issue-group"
[Instance C - Consumer: coupon-consumer-3@server-c] ─┘

Stream 메시지: [msg1, msg2, msg3, msg4, msg5, msg6]
→ msg1: Instance A
→ msg2: Instance B
→ msg3: Instance C
→ msg4: Instance A
→ msg5: Instance B
→ msg6: Instance C
```

**코드 위치**: `CouponIssueQueueConsumer.java:109-141`

#### 3.2.2 재고 및 중복 검증 (Source of Truth = MySQL)

Redis 필터링을 통과한 요청도 MySQL에서 **다시 한번 검증**합니다.

**검증 항목** (트랜잭션 내에서 처리):
1. 사용자 쿠폰 중복 발급 여부 확인
   - `user_coupon` 테이블에서 `(userId, couponId)` 조합 존재 여부 체크
2. 쿠폰 재고 확인
   - `coupon` 테이블의 `issued_count < total_count` 확인
3. 쿠폰 재고 차감
   - `issued_count` 증가 (낙관적 락 또는 비관적 락 사용)
4. 사용자에게 쿠폰 발급
   - `user_coupon` 테이블에 레코드 삽입

**트랜잭션 격리 수준**: `REPEATABLE READ`

**코드 위치**: `CouponIssueTransactionService.java`

#### 3.2.3 Redis 동기화 (필터링 용도)

MySQL 저장 **성공 후**에만 Redis를 업데이트하여 Producer의 필터링 데이터를 최신화합니다.

**업데이트 항목**:
1. **Redis Set에 사용자 추가** (`SADD coupon:issued:users:{couponId} {userId}`)
   - Producer가 중복 요청을 필터링할 때 참조하는 데이터
2. **Redis 재고 차감** (`DECR coupon:stock:{couponId}`)
   - Producer가 진입 제어할 때 참조하는 데이터

**처리 순서** (중요):
```
1. MySQL 트랜잭션 처리 (쿠폰 발급)
2. ✅ 성공 시: Redis 업데이트 (필터링 데이터 동기화)
3. ❌ 실패 시: Redis 업데이트하지 않음 (정합성 유지)
```

**핵심 원칙**:
- ⚠️ **Source of Truth는 MySQL**
- ✅ **Redis는 필터링 용도**로만 사용
- ✅ **Consumer만 Redis를 업데이트**함 (Producer는 참조만)
- ✅ MySQL 성공 → Redis 업데이트 순서로 정합성 보장

**코드 위치**: `CouponIssueQueueConsumer.java:160-162`

#### 3.2.4 메시지 ACK 처리

모든 처리가 성공한 경우에만 Redis Stream에 ACK를 보냅니다.

```
명령어: XACK coupon:issue:queue coupon-issue-group {messageId}
```

**ACK를 보내지 않는 경우**:
- MySQL 저장 실패
- 예외 발생

**결과**: 메시지가 Pending 상태로 유지되어 재처리 대상이 됩니다.

**코드 위치**: `CouponIssueQueueConsumer.java:164`

#### 3.2.5 Pending 메시지 재처리

ACK되지 않은 메시지를 주기적으로 재처리합니다.

**재처리 방식**:
- 각 Consumer가 **자신에게 할당된 Pending 메시지**만 조회
- Consumer Name이 고정되어 있으므로 실패한 메시지 추적 가능
- 10초마다 Scheduler가 자동 실행

**코드 위치**:
- `CouponIssueQueueConsumer.java:181-224`
- `CouponIssueScheduler.java:40-50`
