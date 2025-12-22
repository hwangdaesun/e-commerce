# 선착순 쿠폰 발급 시스템 설계서

## 1. 개요 (Overview)

### 1.1 목적
대규모 트래픽 상황에서 선착순 쿠폰 발급의 **순서 보장(Fairness)** 및 **DB 부하 격리(Backpressure)**를 달성합니다.

### 1.2 기존 문제점
- **분산락 방식의 한계**:
  - 락 획득 경쟁(Race Condition)으로 인한 순서 역전 가능성
  - DB에 직접적인 트래픽 전달로 부하 발생

### 1.3 개선 방향
**Kafka 기반 Producer-Consumer 패턴**을 도입하여:
- 요청을 Kafka Topic에 **직렬화하여 저장**
- MySQL이 **처리 가능한 속도**로 소비(Consume)하여 처리

**개선 효과**:
- ✅ 선착순 보장 (파티션 내 메시지 순서 보장)
- ✅ 부하 조절 가능 (Consumer가 처리 속도 제어)
- ✅ 동시성 문제 원천 차단 (파티션 기반 직렬화)
- ✅ 메시지 영속성 보장 (디스크 저장 및 복제)
- ✅ 확장성 (파티션 수평 확장)

---

## 2. 핵심 아키텍처 (Architecture)

시스템은 크게 **요청 적재(Enqueue)**와 **요청 처리(Dequeue & Process)** 두 단계로 구성됩니다.

### 2.1 기술 스택 선정: Redis Stream vs Kafka

| 비교 항목 | Redis Stream | Kafka |
|----------|-------------|-------|
| **순서 보장** | O (Stream ID 기반) | **O (파티션 내 오프셋 기반)** |
| **데이터 영속성** | 메모리 기반 (디스크 백업 옵션) | **디스크 기반 (복제 및 장기 보관)** |
| **처리량** | 높음 (단일 인스턴스) | **매우 높음 (분산 처리)** |
| **확장성** | 제한적 (수직 확장) | **뛰어남 (수평 확장, 파티션 추가)** |
| **Consumer Group** | 기본 지원 | **기본 지원 (파티션 단위 할당)** |
| **재처리** | Pending 메시지 재처리 | **오프셋 기반 재처리 (세밀한 제어)** |
| **메시지 유실 방지** | ACK 기반 | **오프셋 커밋 + 복제** |
| **운영 복잡도** | 낮음 | 중간 (별도 클러스터 필요) |
| **추천** | 간단한 메시징 | **대규모 이벤트 스트리밍** |

**선택: Kafka**
- 쿠폰 발급은 금전적 가치가 있는 중요한 작업 → **메시지 유실 절대 불가**
- 디스크 기반 영속성 및 복제를 통한 **높은 내구성(Durability)** 보장
- 파티션 기반 확장으로 **대규모 트래픽 대응** 가능
- 동일 쿠폰 요청을 같은 파티션으로 라우팅하여 **선착순 보장**
- Consumer Group을 통한 **멀티 인스턴스 자동 로드밸런싱**
- 오프셋 기반 재처리로 **장애 복구 용이**

---

## 3. 상세 프로세스 (Detailed Flow)

### Phase 1. 요청 적재 (Producer: API Server)

사용자 요청을 받아 대기열에 넣는 단계입니다. **DB 부하가 전혀 발생하지 않습니다.**

#### 3.1.1 쿠폰 유효성 검증 및 재고 조회
```
목적: 유효하지 않은 쿠폰이나 재고가 없는 쿠폰 요청을 사전에 필터링
방법: MySQL에서 쿠폰 정보 및 남은 재고 확인 (참조용)
```

**구현**:
- MySQL에서 쿠폰 유효성 검증 (`CouponService.validateAndGetRemainingQuantity`)
  - 쿠폰 존재 여부 확인
  - 쿠폰 상태 확인 (활성화 여부)
  - 쿠폰 발급 기간 확인
- MySQL에서 남은 재고 수량 조회 (`CouponStock` 조회)

**중요**:
- ⚠️ **Producer는 재고를 참조만 함** (차감하지 않음)
- ✅ **실제 재고 차감은 Consumer에서 MySQL 트랜잭션 내에서 수행**
- MySQL 재고는 **필터링 용도**로만 사용 (Source of Truth는 MySQL)

**코드 위치**: `CouponIssueUseCase.java:35-36`, `CouponService.java`

#### 3.1.2 중복 요청 필터링 (1차 필터링)
```
목적: 동일 사용자의 중복 진입 방지
방법: Redis Set 확인 (참조용)
```

**구현**:
- Redis Set에서 사용자 발급 이력 확인 (`SISMEMBER coupon:issued:users:{couponId} {userId}`)
- 이미 발급받은 사용자는 Kafka 진입 거부

**중요**:
- ⚠️ **Producer는 Redis Set을 조회만 함** (추가하지 않음)
- ✅ **실제 Set 추가는 Consumer에서 MySQL 처리 후 수행**
- Redis Set은 **필터링 용도**로만 사용 (Source of Truth는 MySQL)

**코드 위치**: `CouponIssueProducer.java:86-90`

#### 3.1.3 재고 확인 (2차 필터링)
```
목적: Redis SET 크기 확인으로 재고 필터링
방법: Redis SET의 SCARD로 현재 발급된 개수 확인
```

**구현**:
- Redis SET 크기 조회 (`SCARD coupon:issued:users:{couponId}`)
- 현재 발급된 개수가 남은 재고(remainingQuantity) 이상이면 Kafka 진입 거부

**중요**:
- ⚠️ **2차 필터링으로 대부분의 재고 초과 요청을 사전 차단**
- ✅ **실제 재고 차감은 Consumer에서 MySQL 트랜잭션 내에서 수행**
- Redis SET 크기는 **참고용 지표**로만 사용

**코드 위치**: `CouponIssueProducer.java:47-52`

#### 3.1.4 Kafka 메시지 전송
```
목적: 쿠폰 발급 요청을 Kafka Topic에 전송
자료구조: Kafka Topic (coupon-issue-topic)
파티셔닝 전략: couponId를 key로 사용
```

**메시지 구조**:
```java
public class CouponIssueMessage {
    private Long userId;
    private Long couponId;
}
```

**파티셔닝 전략**:
- **couponId를 메시지 key로 사용**하여 같은 쿠폰은 항상 같은 파티션으로 전송
- 동일 파티션 내에서는 메시지 순서가 보장됨 → **선착순 보장**
- 서로 다른 쿠폰은 다른 파티션에 분산 → **병렬 처리 가능**

#### 3.1.5 사용자 응답

실제 발급은 Consumer에서 비동기로 처리되며, 사용자는 발급된 쿠폰을 별도 조회 API로 확인합니다.

---

### Phase 2. 요청 처리 (Consumer: Worker Server)

줄 서 있는 요청을 하나씩 꺼내어 실제 DB에 반영하는 단계입니다.

#### 3.2.1 Kafka 메시지 수신
```
어노테이션: @KafkaListener
Topic: coupon-issue-topic
Consumer Group: coupon-issue-consumer-group
컨테이너 팩토리: kafkaListenerContainerFactory
```

**Consumer Group 전략**:
- **모든 인스턴스가 동일한 Consumer Group 사용**
  - 같은 메시지가 여러 인스턴스에 중복 전달되지 않음
  - Kafka가 자동으로 파티션을 Consumer에게 할당
  - 멀티 인스턴스 환경에서 로드 밸런싱 자동 수행

**파티션 할당 전략**:
- Kafka는 **파티션 단위로 Consumer에게 메시지 할당**
  - 각 파티션은 동일 Consumer Group 내 하나의 Consumer에게만 할당
  - 파티션 내에서는 순서가 보장됨 (오프셋 순서대로 처리)

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

**처리 순서** (중요):
```
1. MySQL 트랜잭션 처리 (쿠폰 발급) - CouponIssueTransactionService.issueCoupon()
2. ✅ 성공 시: Redis Set에 사용자 추가 (필터링 데이터 동기화)
3. ❌ 실패 시: Redis 업데이트하지 않음 (정합성 유지)
```

**핵심 원칙**:
- ⚠️ **Source of Truth는 MySQL**
- ✅ **Redis는 필터링 용도**로만 사용
- ✅ **Consumer만 Redis를 업데이트**함 (Producer는 참조만)
- ✅ MySQL 성공 → Redis 업데이트 순서로 정합성 보장

**코드 위치**: `CouponIssueConsumer.java:63-66`

#### 3.2.4 Kafka 오프셋 커밋 (Acknowledgment)

모든 처리가 성공한 경우에만 Kafka에 오프셋을 커밋합니다.

**오프셋 커밋 방식**:
- **수동 커밋 모드** (MANUAL_IMMEDIATE 또는 MANUAL) 사용
- MySQL 저장 및 Redis 업데이트가 모두 성공한 경우에만 `acknowledgment.acknowledge()` 호출
- 커밋되면 해당 오프셋의 메시지는 처리 완료로 간주

**커밋하지 않는 경우**:
- MySQL 저장 실패
- Redis 업데이트 실패
- 예외 발생

**결과**:
- 오프셋이 커밋되지 않으면 Consumer 재시작 시 **해당 오프셋부터 다시 처리**
- Kafka는 커밋된 마지막 오프셋부터 메시지를 재전송

---

## 4. 전체 시퀀스 다이어그램 (Sequence Diagram)

```
[사용자] → [API Server (Producer)]
                ↓
        1. 쿠폰 유효성 검증 (MySQL)
                ↓
        2. Redis Set 중복 확인 (SISMEMBER)
                ↓
        3. Redis SET 크기 재고 확인 (SCARD)
                ↓
        4. Kafka 메시지 전송 (couponId를 key로 파티셔닝)
                ↓
         [Kafka Topic]
                ↓
    [Consumer (Worker Server)]
                ↓
        5. Kafka 메시지 수신 (@KafkaListener)
                ↓
        6. MySQL 트랜잭션 (쿠폰 발급)
           - 중복 발급 검증
           - 재고 확인 및 차감
           - user_coupon 테이블 삽입
                ↓
        7. Redis Set에 사용자 추가 (SADD)
                ↓
        8. Kafka 오프셋 커밋 (acknowledgment.acknowledge())
```

---

## 5. 핵심 설계 원칙 (Key Design Principles)

### 5.1 선착순 보장
- **couponId를 Kafka 메시지 key로 사용**하여 같은 쿠폰은 항상 같은 파티션으로 전송
- 파티션 내에서는 메시지 순서가 보장됨 (오프셋 순서대로 처리)

### 5.2 데이터 정합성
- **Source of Truth는 MySQL**
- Redis는 필터링 용도로만 사용
- MySQL 성공 → Redis 업데이트 순서로 정합성 보장

### 5.3 메시지 유실 방지
- **수동 커밋 모드** 사용
- MySQL 및 Redis 처리가 모두 성공한 경우에만 오프셋 커밋
- 실패 시 재처리 보장

### 5.4 멱등성
- MySQL 유니크 제약으로 중복 발급 방지
- Redis SADD의 멱등성 활용

---

## 6. 장점 및 고려사항

### 6.1 장점
✅ **선착순 보장**: 파티션 내 메시지 순서 보장
✅ **부하 격리**: Kafka가 DB 부하를 흡수
✅ **확장성**: 파티션 추가로 수평 확장 가능
✅ **메시지 영속성**: 디스크 기반 저장 및 복제
✅ **장애 복구**: 오프셋 기반 재처리

### 6.2 고려사항
⚠️ **운영 복잡도**: Kafka 클러스터 운영 및 모니터링 필요
⚠️ **지연 시간**: 비동기 처리로 인한 발급 완료 지연
⚠️ **멱등성 설계**: 재처리를 고려한 멱등성 보장 필요
