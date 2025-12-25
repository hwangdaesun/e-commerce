# Grafana + Prometheus 모니터링 시스템 구축 완료

## 개요

HHPlus E-Commerce 프로젝트에 Prometheus + Grafana 기반 모니터링 시스템을 성공적으로 구축했습니다.

**구축일**: 2025-12-24
**기술 스택**: Spring Boot Actuator, Micrometer, Prometheus, Grafana

---

## 구성 요소

### 1. Spring Boot Actuator 설정

#### 의존성 추가 (build.gradle:44-45)
```gradle
implementation 'org.springframework.boot:spring-boot-starter-actuator'
implementation 'io.micrometer:micrometer-registry-prometheus'
```

#### 설정 파일 (application.yaml:59-80)
```yaml
management:
  endpoints:
    web:
      exposure:
        include: health,info,prometheus,metrics
  metrics:
    export:
      prometheus:
        enabled: true
```

**접속 URL**:
- Health: http://localhost:8080/actuator/health
- Metrics: http://localhost:8080/actuator/metrics
- Prometheus: http://localhost:8080/actuator/prometheus

### 2. Prometheus 설정

#### 설정 파일 위치
```
monitoring/prometheus/prometheus.yml
```

#### 주요 설정
- **Scrape Interval**: 10초
- **Target**: Spring Boot 애플리케이션 (host.docker.internal:8080)
- **Metrics Path**: /actuator/prometheus

**접속 URL**: http://localhost:9090

### 3. Grafana 설정

#### 자동 프로비저닝
```
monitoring/grafana/provisioning/
├── datasources/
│   └── prometheus.yml          # Prometheus 데이터소스 자동 추가
└── dashboards/
    ├── dashboard.yml           # 대시보드 프로비저닝 설정
    └── json/
        └── spring-boot-overview.json  # Spring Boot 대시보드
```

**접속 URL**: http://localhost:3000
**로그인**: admin / admin

### 4. Docker Compose 통합

#### 추가된 서비스 (docker-compose.yml:68-103)
```yaml
prometheus:
  image: prom/prometheus:latest
  ports:
    - "9090:9090"

grafana:
  image: grafana/grafana:latest
  ports:
    - "3000:3000"
```

#### 실행 명령
```bash
docker-compose up -d prometheus grafana
```

---

## 수집 메트릭

### 기본 메트릭 (Spring Actuator 제공)

#### 1. HTTP 요청 메트릭
- `http_server_requests_seconds_count`: 요청 수
- `http_server_requests_seconds_sum`: 총 처리 시간
- `http_server_requests_seconds_bucket`: 응답 시간 히스토그램

**태그**: uri, method, status

**쿼리 예시**:
```promql
# TPS (초당 요청 수)
rate(http_server_requests_seconds_count[1m])

# P95 응답 시간 (ms)
histogram_quantile(0.95, sum(rate(http_server_requests_seconds_bucket[5m])) by (le)) * 1000

# 5xx 에러율
sum(rate(http_server_requests_seconds_count{status=~"5.."}[1m])) / sum(rate(http_server_requests_seconds_count[1m])) * 100
```

#### 2. JVM 메트릭
- `jvm_memory_used_bytes`: 메모리 사용량
- `jvm_memory_max_bytes`: 최대 메모리
- `jvm_gc_pause_seconds_count`: GC 발생 횟수
- `jvm_threads_live_threads`: 활성 스레드 수

**쿼리 예시**:
```promql
# Heap 메모리 사용률
(jvm_memory_used_bytes{area="heap"} / jvm_memory_max_bytes{area="heap"}) * 100

# GC 발생 빈도
rate(jvm_gc_pause_seconds_count[5m])
```

#### 3. 시스템 메트릭
- `system_cpu_usage`: 시스템 CPU 사용률
- `process_cpu_usage`: 프로세스 CPU 사용률

#### 4. 데이터베이스 메트릭 (HikariCP)
- `hikaricp_connections_active`: 활성 커넥션
- `hikaricp_connections_idle`: 유휴 커넥션
- `hikaricp_connections_max`: 최대 커넥션
- `hikaricp_connections_pending`: 대기 중 요청

**쿼리 예시**:
```promql
# 커넥션 풀 사용률
(hikaricp_connections_active / hikaricp_connections_max) * 100
```

### 커스텀 비즈니스 메트릭

#### CustomMetrics 클래스 제공 (src/.../common/monitoring/CustomMetrics.java)

#### 1. 쿠폰 발급 메트릭
```java
customMetrics.incrementCouponIssueRequest(couponId);     // 요청 수
customMetrics.incrementCouponIssueSuccess(couponId);     // 성공 수
customMetrics.incrementCouponIssueFail(couponId, reason); // 실패 수
customMetrics.recordCouponIssueTime(couponId, timeMs);    // 처리 시간
```

**메트릭 이름**:
- `coupon_issue_requests_total`
- `coupon_issue_success_total`
- `coupon_issue_fail_total`
- `coupon_issue_duration_seconds`

**쿼리 예시**:
```promql
# 쿠폰 발급 성공률
rate(coupon_issue_success_total[5m]) / rate(coupon_issue_requests_total[5m]) * 100

# 쿠폰 발급 평균 처리 시간
rate(coupon_issue_duration_seconds_sum[5m]) / rate(coupon_issue_duration_seconds_count[5m]) * 1000
```

#### 2. 주문 메트릭
```java
customMetrics.incrementOrderCreate(status);              // 주문 생성
customMetrics.incrementOrderPaymentComplete();           // 결제 완료
customMetrics.recordOrderProcessingTime(timeMs);         // 처리 시간
```

**메트릭 이름**:
- `order_create_total`
- `order_payment_complete_total`
- `order_processing_duration_seconds`

#### 3. 재고 메트릭
```java
customMetrics.incrementStockDecrease(productId, quantity); // 재고 차감
customMetrics.incrementStockShortage(productId);           // 재고 부족
```

**메트릭 이름**:
- `stock_decrease_total`
- `stock_shortage_total`

#### 4. Kafka Consumer 메트릭
```java
customMetrics.incrementKafkaMessageSuccess(topic, group);        // 처리 성공
customMetrics.incrementKafkaMessageFail(topic, group, errorType); // 처리 실패
customMetrics.recordKafkaMessageProcessingTime(topic, timeMs);    // 처리 시간
```

**메트릭 이름**:
- `kafka_message_success_total`
- `kafka_message_fail_total`
- `kafka_message_processing_duration_seconds`

**쿼리 예시**:
```promql
# Kafka Consumer 처리율
rate(kafka_message_success_total[5m])

# Kafka Consumer 에러율
rate(kafka_message_fail_total[5m]) / rate(kafka_message_success_total[5m] + kafka_message_fail_total[5m]) * 100
```

---

## Grafana 대시보드

### Spring Boot 애플리케이션 개요 대시보드

자동 프로비저닝된 대시보드는 다음 패널을 포함합니다:

1. **HTTP 요청률 (Requests/sec)**
   - API 엔드포인트별 초당 요청 수 추이
   - Time Series 그래프

2. **HTTP 응답 시간 P95 (ms)**
   - 95번째 백분위수 응답 시간
   - Gauge 시각화
   - 임계값: 100ms (노란색), 500ms (빨간색)

3. **HTTP 응답 시간 분포 (P50/P95/P99)**
   - URI별 P50, P95, P99 응답 시간 비교
   - Time Series 그래프

4. **JVM Heap 메모리 사용량**
   - Heap Used vs Heap Max
   - Time Series 그래프

5. **HikariCP 커넥션 풀**
   - Active, Idle, Max 커넥션 수
   - Time Series 그래프

6. **CPU 사용률**
   - System CPU vs Process CPU
   - Time Series 그래프

7. **애플리케이션 상태**
   - Up/Down 상태 표시
   - Gauge 시각화

### 대시보드 접속 방법

1. http://localhost:3000 접속
2. admin / admin 로그인
3. Dashboards → HHPlus E-Commerce 폴더
4. "HHPlus E-Commerce - Spring Boot 애플리케이션 개요" 선택

---

## 사용 방법

### 1. 시스템 시작

```bash
# 1. 모니터링 스택 실행
docker-compose up -d prometheus grafana

# 2. Spring Boot 애플리케이션 실행
./gradlew bootRun

# 3. 상태 확인
curl http://localhost:8080/actuator/health
curl http://localhost:9090/targets
```

### 2. 커스텀 메트릭 추가

#### 예시 1: 쿠폰 발급 서비스
```java
@Service
@RequiredArgsConstructor
public class CouponService {

    private final CustomMetrics customMetrics;

    public void issueCoupon(Long userId, Long couponId) {
        long startTime = System.currentTimeMillis();

        try {
            customMetrics.incrementCouponIssueRequest(couponId.toString());

            // 비즈니스 로직
            // ...

            customMetrics.incrementCouponIssueSuccess(couponId.toString());

        } catch (StockShortageException e) {
            customMetrics.incrementCouponIssueFail(couponId.toString(), "stock_shortage");
            throw e;
        } catch (DuplicateIssueException e) {
            customMetrics.incrementCouponIssueFail(couponId.toString(), "duplicate");
            throw e;
        } finally {
            long duration = System.currentTimeMillis() - startTime;
            customMetrics.recordCouponIssueTime(couponId.toString(), duration);
        }
    }
}
```

#### 예시 2: @Timed 애노테이션 사용
```java
@Service
public class OrderService {

    @Timed(value = "order.create", description = "주문 생성 처리 시간")
    public Order createOrder(OrderRequest request) {
        // 주문 생성 로직
        // ...
    }
}
```

### 3. 새 대시보드 패널 추가

#### Grafana에서 패널 생성
1. 대시보드 우측 상단 **Add panel** 클릭
2. PromQL 쿼리 입력:
   ```promql
   # 쿠폰 발급 성공률
   rate(coupon_issue_success_total[5m]) / rate(coupon_issue_requests_total[5m]) * 100
   ```
3. 시각화 타입 선택 (Time series, Gauge, Stat 등)
4. 패널 제목 및 설명 작성
5. **Apply** → **Save dashboard**

---

## 모니터링 권장 사항

### 1. 핵심 메트릭 모니터링

#### 성능 메트릭
- **API 응답 시간**: P95 < 200ms, P99 < 500ms
- **TPS**: 목표 처리량 대비 현재 처리량
- **에러율**: 5xx 에러 < 1%

#### 리소스 메트릭
- **Heap 메모리**: 사용률 < 90%
- **CPU 사용률**: 평균 < 70%, 피크 < 90%
- **DB 커넥션 풀**: 사용률 < 80%

#### 비즈니스 메트릭
- **쿠폰 발급 성공률**: > 95%
- **주문 처리 성공률**: > 99%
- **재고 부족 발생률**: < 5%

### 2. 알림 설정 권장

| 메트릭 | 임계값 | 심각도 |
|--------|--------|--------|
| HTTP 5xx 에러율 | > 5% | Critical |
| API P95 응답 시간 | > 500ms | Warning |
| Heap 메모리 사용률 | > 90% | Critical |
| DB 커넥션 풀 사용률 | > 80% | Warning |
| 쿠폰 발급 실패율 | > 10% | Warning |
| Kafka Consumer Lag | > 1000 | Warning |

### 3. 대시보드 검토 주기

- **실시간**: 부하 테스트 시
- **매일**: HTTP 요청 추이, 에러율, 응답 시간
- **매주**: 리소스 사용 추세, GC 빈도, 비즈니스 메트릭
- **매월**: 장기 트렌드 분석, 용량 계획

---

## 성능 테스트 시 활용

### 부하 테스트 전 체크리스트

1. ✅ Prometheus가 메트릭을 정상 수집 중인지 확인
2. ✅ Grafana 대시보드가 실시간으로 업데이트되는지 확인
3. ✅ 모든 커스텀 메트릭이 수집되는지 확인

### 부하 테스트 중 모니터링

다음 메트릭을 실시간으로 확인:

1. **HTTP 요청률**: VUs 증가에 따른 TPS 변화
2. **응답 시간 분포**: P50/P95/P99 추이
3. **에러율**: 5xx 에러 발생 빈도
4. **JVM Heap**: 메모리 사용량 증가 추세
5. **DB 커넥션**: Active 커넥션 수
6. **Kafka Consumer Lag**: 메시지 처리 지연

### 부하 테스트 후 분석

1. **성능 리포트 생성**:
   - Grafana 대시보드 스크린샷 저장
   - 주요 메트릭 통계 추출 (평균, 최대, P95, P99)

2. **병목 구간 파악**:
   - 응답 시간이 급증한 시점 분석
   - 리소스 사용률 피크 시점 확인
   - 에러 발생 패턴 분석

---

## 문제 해결

### 메트릭이 수집되지 않음

**증상**: Grafana 대시보드가 비어있거나 "No data" 표시

**해결 방법**:
1. Spring Boot 애플리케이션 상태 확인:
   ```bash
   curl http://localhost:8080/actuator/prometheus
   ```

2. Prometheus Targets 확인:
   - http://localhost:9090/targets
   - `hhplus-ecommerce` 타겟이 UP 상태인지 확인

3. Prometheus 로그 확인:
   ```bash
   docker logs ecommerce-prometheus
   ```

### 커스텀 메트릭이 표시되지 않음

**원인**: 메트릭을 기록하는 코드가 아직 실행되지 않음

**해결 방법**:
1. 해당 기능 호출:
   ```bash
   # 쿠폰 발급 API 호출
   curl -X POST http://localhost:8080/api/coupons/1/issue
   ```

2. Actuator에서 메트릭 확인:
   ```bash
   curl http://localhost:8080/actuator/prometheus | grep "coupon_issue"
   ```

### Grafana 접속 불가

**해결 방법**:
```bash
# 컨테이너 상태 확인
docker ps | grep grafana

# 로그 확인
docker logs ecommerce-grafana

# 재시작
docker-compose restart grafana
```

---

## 추가 확장 계획

### 1. Alertmanager 통합

Prometheus 알림을 Slack, Email 등으로 전송

```yaml
# prometheus.yml에 추가
alerting:
  alertmanagers:
    - static_configs:
        - targets:
          - alertmanager:9093
```

### 2. 추가 Exporter

- **MySQL Exporter**: DB 메트릭 수집
- **Redis Exporter**: 캐시 메트릭 수집
- **Kafka Exporter**: Kafka 메트릭 수집

### 3. 로그 통합 (ELK Stack)

Prometheus/Grafana와 함께 ELK Stack 구축하여 메트릭 + 로그 통합 모니터링

---

## 참고 문서

- [모니터링 README](../monitoring/README.md): 상세 가이드
- [빠른 시작 가이드](../monitoring/QUICKSTART.md): 5분 실행 가이드
- [성능 테스트 보고서](./performance-test-report-20251224.md)
- [쿠폰 발급 성능 테스트](coupon-issue-performance-test-report.md)

---

**구축 완료일**: 2025-12-24
**작성자**: HHPlus E-Commerce Team
**버전**: 1.0
