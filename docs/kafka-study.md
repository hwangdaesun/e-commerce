# Kafka Study

## 왜 Kafka를 사용하는가?

### 1. 관심사의 분리 (Separation of Concerns)
Kafka를 사용하는 가장 핵심적인 이유는 **관심사의 분리**입니다.

### 2. MSA 환경에서의 복잡한 의존 관계 문제
서비스의 규모가 커지면서 Microservices Architecture(MSA) 형태로 서비스를 분리하기 시작했습니다.
이 과정에서 서비스 간 의존 관계는 점점 더 복잡해지게 되었습니다.

**문제점:**
- 서비스 A → 서비스 B → 서비스 C 와 같은 체인 형태의 의존성
- 한 서비스의 변경이 여러 서비스에 영향을 미침
- 직접적인 HTTP 통신으로 인한 강한 결합(Tight Coupling)

### 3. 비동기 통신을 통한 느슨한 결합
서비스 간 통신을 할 때 **최대한 의존을 줄이기 위해** 비동기적으로 통신을 수행하고자 합니다.

**Kafka의 해결 방식:**
- 직접적인 서비스 간 연결을 피함
- 요청/응답 스펙을 정의하지 않음
- **이벤트 기반 통신**: 이벤트를 발행(Publish)하고 컨슘(Consume)하는 방식

```
[기존 방식]
서비스 A --HTTP 요청--> 서비스 B (강한 결합)

[Kafka 방식]
서비스 A --이벤트 발행--> Kafka <--이벤트 구독-- 서비스 B (느슨한 결합)
```

### 4. 이벤트 유실 방지
이벤트 자체가 유실되지 않기를 원하기 때문에 Kafka의 특징을 활용합니다:

- **분산 메시지 처리**: 여러 브로커를 통한 안정적인 메시지 전달
- **디스크 저장**: 메모리가 아닌 디스크에 메시지를 저장하여 영속성 보장
- **복제(Replication)**: 데이터를 여러 브로커에 복제하여 장애 대응

### 정리
| 요구사항 | Kafka의 해결 방법 |
|---------|-----------------|
| 관심사의 분리 | 이벤트 기반 아키텍처 |
| 복잡한 의존 관계 해소 | Pub/Sub 패턴으로 느슨한 결합 |
| 비동기 통신 | 메시지 큐 방식의 비동기 처리 |
| 이벤트 유실 방지 | 디스크 저장 + 복제 + 분산 처리 |

---

## Kafka의 구성

### 장애 대비 전략
Kafka와 같은 분산 서비스 도구들은 **클러스터링(Clustering)**과 **레플리케이션(Replication)** 구성을 통해 시스템 장애를 대비합니다.

### 계층 구조
Kafka는 다음과 같은 계층적 구조로 구성됩니다:

```
Kafka Cluster
  └─ Broker (1..N)
      └─ Topic (1..N)
          └─ Partition (1..N)
```

#### 1. Kafka Cluster
- 여러 Broker로 구성된 Kafka 시스템 전체
- 고가용성과 확장성을 제공

#### 2. Broker
- Kafka 서버 인스턴스
- 메시지를 저장하고 전달하는 역할
- 클러스터 내에서 여러 Broker가 분산 처리

#### 3. Topic
- **역할**: 이벤트의 종류를 구별하는 식별자
- **사용처**: Producer와 Consumer가 메시지를 분류하기 위해 사용
- 예: `order-created`, `stock-reserved`, `payment-completed`

#### 4. Partition
- **역할**: 대용량 트래픽을 병렬적으로 빠르게 처리
- **특징**: Partition 개수만큼 병렬 처리 가능

```
Topic: order-created
  ├─ Partition 0: [메시지1, 메시지4, 메시지7] ← 순차 처리
  ├─ Partition 1: [메시지2, 메시지5, 메시지8] ← 순차 처리
  └─ Partition 2: [메시지3, 메시지6, 메시지9] ← 순차 처리
```

### 순서 보장의 중요한 특성

**주의사항:**
- **각 Partition 내부**: 순차 처리 보장 ✅
- **Topic 전체**: 순차 처리 보장 안 됨 ❌

**이유:**
- 하나의 Topic에 여러 Partition이 있기 때문
- 각 Partition은 독립적으로 병렬 처리됨
- Partition 간의 순서는 보장되지 않음

**순서가 중요한 경우:**
- Partition 개수를 1개로 설정
- 또는 특정 Key를 기준으로 같은 Partition으로 라우팅 (예: userId, orderId)

### Consumer Group

하나의 Topic에 발행된 메시지를 **여러 서비스가 독립적으로 컨슘**하기 위해 Consumer Group을 사용합니다.

```
Topic: order-created (3 Partitions)

Consumer Group A (주문 서비스)
  ├─ Consumer A-1 → Partition 0
  ├─ Consumer A-2 → Partition 1
  └─ Consumer A-3 → Partition 2

Consumer Group B (알림 서비스)
  ├─ Consumer B-1 → Partition 0, 1
  └─ Consumer B-2 → Partition 2
```

**특징:**
- 같은 Consumer Group 내에서는 하나의 메시지를 하나의 Consumer만 처리
- 다른 Consumer Group은 같은 메시지를 독립적으로 처리 가능

### Partition과 Consumer 매핑 전략

**베스트 프랙티스: 1 Partition : 1 Consumer**
```
Partition 0 ─── Consumer 1
Partition 1 ─── Consumer 2
Partition 2 ─── Consumer 3
```
- 최적의 병렬 처리
- 각 Consumer가 독립적으로 동작

**가능한 구성: N Partition : 1 Consumer**
```
Partition 0 ─┐
Partition 1 ─┼─ Consumer 1
Partition 2 ─┘
```
- Consumer가 Partition보다 적을 때
- 하나의 Consumer가 여러 Partition 처리
- 처리 속도는 느려질 수 있음

**불가능한 구성: 1 Partition : N Consumer** ❌
```
              ┌─ Consumer 1
Partition 0 ──┼─ Consumer 2  (X 불가능)
              └─ Consumer 3
```
- 같은 Consumer Group 내에서는 불가능
- 메시지 중복 처리 문제 발생

### 구성 요약
| 구성 요소 | 역할 | 특징 |
|---------|------|------|
| Kafka Cluster | 전체 시스템 | 고가용성, 확장성 |
| Broker | 메시지 저장/전달 | 분산 처리 |
| Topic | 이벤트 식별자 | 메시지 분류 |
| Partition | 병렬 처리 단위 | 개별 순서 보장, 전체 순서 보장 안 됨 |
| Consumer Group | 독립적 컨슘 | 여러 서비스의 메시지 구독 |

---

## HA(High Availability) 보장 방법

Kafka는 **고가용성**을 보장하기 위해 레플리케이션(Replication) 메커니즘을 사용합니다.

### 1. Broker 구성 전략

**3개 이상의 홀수개 Broker 사용**
```
Kafka Cluster
  ├─ Broker 1
  ├─ Broker 2
  └─ Broker 3
```

**홀수개를 사용하는 이유:**
- 과반수(Quorum) 기반 합의 알고리즘 사용
- 장애 발생 시 과반수 Broker가 살아있으면 정상 동작
- 짝수개의 경우 Split-brain 문제 발생 가능

**예시:**
- 3개 Broker: 1개 장애 허용 (2개 생존 시 정상)
- 5개 Broker: 2개 장애 허용 (3개 생존 시 정상)

### 2. Leader-Follower 구조

각 **Topic의 Partition**마다 **Leader Partition 1개**와 **Follower Partition(들)**이 존재합니다.

```
Topic: order-created, Partition 0, Replication Factor: 3

Broker 1: [Leader Partition 0]      ← Producer/Consumer는 Leader와만 통신
Broker 2: [Follower Partition 0]    ← Leader의 데이터 복제
Broker 3: [Follower Partition 0]    ← Leader의 데이터 복제
```

#### Leader Partition
- **역할**: Producer로부터 메시지를 받고, Consumer에게 메시지를 전달
- **특징**: 읽기/쓰기 모두 Leader를 통해서만 수행
- **개수**: Partition당 1개

#### Follower Partition
- **역할**: Leader의 메시지를 복제하여 백업
- **특징**: 단순 복제만 수행, 읽기/쓰기 처리 안 함
- **개수**: Replication Factor - 1개

### 3. 장애 복구 프로세스 (Failover)

**정상 상태:**
```
Broker 1: [Leader P0]     ← Producer/Consumer 통신
Broker 2: [Follower P0]   ← 복제
Broker 3: [Follower P0]   ← 복제
```

**Leader 장애 발생:**
```
Broker 1: [Leader P0] ✗ 중단
Broker 2: [Follower P0] → [Leader P0] ← 승격
Broker 3: [Follower P0]
```

**복구 과정:**
1. Broker 1의 Leader Partition 0이 중단됨
2. Kafka Controller가 장애를 감지
3. ISR(In-Sync Replicas) 목록에서 새로운 Leader 선출
4. Broker 2의 Follower가 Leader로 승격
5. Producer/Consumer는 자동으로 새 Leader와 통신

### 4. 주요 개념

#### ISR (In-Sync Replicas)
- Leader와 동기화 상태를 유지하는 Replica 목록
- Leader 장애 시 ISR 내의 Follower만 Leader로 선출 가능
- 동기화가 늦어진 Follower는 ISR에서 제외됨

#### Replication Factor
- Partition의 복제본 개수
- `Replication Factor = 3`: Leader 1개 + Follower 2개
- 높을수록 안정성 증가, 저장 공간 증가

#### Min In-Sync Replicas
- 최소 동기화 상태 유지가 필요한 Replica 수
- 예: `min.insync.replicas=2`이면 Leader + 최소 1개 Follower가 동기화되어야 쓰기 성공

### 5. HA 구성 예시

**실무 권장 구성:**
```
Kafka Cluster (3 Brokers)
  Topic: order-created
    Replication Factor: 3
    Min In-Sync Replicas: 2

Broker 1:
  - Partition 0: Leader
  - Partition 1: Follower
  - Partition 2: Follower

Broker 2:
  - Partition 0: Follower
  - Partition 1: Leader
  - Partition 2: Follower

Broker 3:
  - Partition 0: Follower
  - Partition 1: Follower
  - Partition 2: Leader
```

**이 구성의 장점:**
- 각 Broker가 일부 Partition의 Leader 역할 → 부하 분산
- 각 Partition이 3개 Broker에 복제 → 2개 Broker 장애까지 복구 가능
- Min In-Sync Replicas=2 → 최소 2개 Broker에 복제 성공해야 쓰기 완료

### HA 보장 요약
| 구성 요소 | 목적 | 설정 예시 |
|---------|------|---------|
| Broker 개수 | 장애 허용 | 3개 이상 홀수 |
| Replication Factor | 데이터 복제 | 3 (운영 환경) |
| Min In-Sync Replicas | 쓰기 안정성 | 2 |
| Leader-Follower | 읽기/쓰기 처리 | Leader: 1개, Follower: N개 |
| ISR | Leader 선출 | 동기화 상태 Replica만 선출 |