#!/bin/bash

# ============================================
# 쿠폰 발급 부하 테스트 + Consumer Lag 모니터링 + 정합성 검증 통합 스크립트
# ============================================
#
# 실행 순서:
#   1. 테스트 데이터 초기화 (init-coupon-issue-test.sql)
#   2. K6 부하 테스트 실행 (coupon-issue-basic-test.js) + Consumer Lag 모니터링
#   3. 지연 처리 추적 (1분, 5분, 10분 간격 체크)
#   4. 데이터 정합성 검증 (coupon-issue-integrity-check.sql)
#   5. 결과 출력 및 저장
#
# 사용법:
#   ./run-coupon-issue-test.sh
#
# 필요 환경변수 (선택):
#   DB_HOST=localhost
#   DB_PORT=3306
#   DB_USER=ecommerce_user
#   DB_PASSWORD=ecommerce_pass
#   DB_NAME=ecommerce
#
# 출력 파일:
#   - k6-coupon-issue-{타임스탬프}.json: K6 테스트 결과
#   - validation-coupon-issue-{타임스탬프}.txt: 정합성 검증 결과
#   - consumer-lag-coupon-issue-{타임스탬프}.log: Consumer Lag 모니터링 로그
#   - delayed-tracking-coupon-issue-{타임스탬프}.txt: 지연 처리 추적 결과
# ============================================

set -e  # 에러 발생 시 즉시 중단

# ============================================
# 설정
# ============================================

# DB 접속 정보 (application.yaml 기반 기본값)
DB_HOST=${DB_HOST:-127.0.0.1}
DB_PORT=${DB_PORT:-3306}
DB_USER=${DB_USER:-ecommerce_user}
DB_PASSWORD=${DB_PASSWORD:-ecommerce_pass}
DB_NAME=${DB_NAME:-ecommerce}

# Kafka 설정
KAFKA_CONTAINER=${KAFKA_CONTAINER:-ecommerce-kafka}
KAFKA_BOOTSTRAP_SERVER=${KAFKA_BOOTSTRAP_SERVER:-localhost:9093}

# Consumer Group (쿠폰 발급)
COUPON_CONSUMER_GROUP="coupon-issue-group"

# 경로 설정
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
DATA_DIR="$SCRIPT_DIR/data"
VALIDATION_DIR="$SCRIPT_DIR/validation"
RESULTS_DIR="$SCRIPT_DIR/results"

# 결과 파일명
TIMESTAMP=$(date +%Y%m%d_%H%M%S)
K6_RESULT_FILE="$RESULTS_DIR/k6-coupon-issue-${TIMESTAMP}.json"
VALIDATION_RESULT_FILE="$RESULTS_DIR/validation-coupon-issue-${TIMESTAMP}.txt"
CONSUMER_LAG_FILE="$RESULTS_DIR/consumer-lag-coupon-issue-${TIMESTAMP}.log"
DELAYED_TRACKING_FILE="$RESULTS_DIR/delayed-tracking-coupon-issue-${TIMESTAMP}.txt"

# 디렉토리 생성
mkdir -p "$RESULTS_DIR"

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m' # No Color

# ============================================
# Consumer Lag 모니터링 함수
# ============================================

# Consumer Lag 수집 함수
collect_consumer_lag() {
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

  {
    echo "[$timestamp]"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
    echo "Consumer Group: $COUPON_CONSUMER_GROUP"
    echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

    docker exec $KAFKA_CONTAINER kafka-consumer-groups \
      --bootstrap-server $KAFKA_BOOTSTRAP_SERVER \
      --group $COUPON_CONSUMER_GROUP \
      --describe 2>&1 | grep -v "WARN" || echo "Group not found or no active members"

    echo ""
  } >> "$CONSUMER_LAG_FILE"
}

# Consumer Lag 실시간 요약 표시
show_lag_summary() {
  echo ""
  echo -e "${CYAN}📊 현재 Consumer Lag 요약${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  TOTAL_LAG=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups \
    --bootstrap-server $KAFKA_BOOTSTRAP_SERVER \
    --group $COUPON_CONSUMER_GROUP \
    --describe 2>&1 | \
    awk 'NR > 1 && NF >= 5 { sum += $5 } END { print sum+0 }' 2>/dev/null || echo "0")

  if [ "$TOTAL_LAG" -eq 0 ]; then
    echo -e "  ${GREEN}✅ ${COUPON_CONSUMER_GROUP}: LAG = 0${NC}"
  elif [ "$TOTAL_LAG" -lt 100 ]; then
    echo -e "  ${YELLOW}⚠️  ${COUPON_CONSUMER_GROUP}: LAG = ${TOTAL_LAG}${NC}"
  else
    echo -e "  ${RED}❌ ${COUPON_CONSUMER_GROUP}: LAG = ${TOTAL_LAG} (병목 발생!)${NC}"
  fi

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
}

# 백그라운드 Consumer Lag 모니터링 프로세스
monitor_consumer_lag() {
  local interval=${1:-2}  # 기본 2초 간격

  echo -e "${BLUE}🔍 Consumer Lag 모니터링 시작 (${interval}초 주기)${NC}" >&2
  echo -e "${BLUE}   로그 파일: $CONSUMER_LAG_FILE${NC}" >&2
  echo "" >&2

  while true; do
    collect_consumer_lag
    show_lag_summary >&2
    sleep $interval
  done
}

# 종료 시 정리 함수
cleanup() {
  echo ""
  echo -e "${YELLOW}🛑 Consumer Lag 모니터링 중지 중...${NC}"

  # 백그라운드 프로세스 종료
  if [ -n "$LAG_MONITOR_PID" ]; then
    kill $LAG_MONITOR_PID 2>/dev/null || true
    wait $LAG_MONITOR_PID 2>/dev/null || true
  fi

  echo -e "${GREEN}✅ 정리 완료${NC}"
}

# 시그널 핸들러 등록
trap cleanup EXIT INT TERM

# ============================================
# MySQL 연결 테스트
# ============================================

echo "=========================================="
echo "🚀 쿠폰 발급 부하 테스트 + 정합성 검증 + Consumer Lag 모니터링"
echo "=========================================="
echo "시작 시간: $(date '+%Y-%m-%d %H:%M:%S')"
echo "=========================================="
echo ""

# MySQL 접속 테스트
echo "📡 MySQL 연결 테스트 중..."
echo "   호스트: $DB_HOST:$DB_PORT"
echo "   사용자: $DB_USER"
echo "   데이터베이스: $DB_NAME"
if ! mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "SELECT 1" > /dev/null 2>&1; then
    echo "❌ MySQL 연결 실패!"
    exit 1
fi
echo "✅ MySQL 연결 성공"
echo ""

# ============================================
# 1단계: 테스트 데이터 초기화
# ============================================

echo "=========================================="
echo "[1/4] 쿠폰 발급 테스트 데이터 초기화 중..."
echo "=========================================="
echo "⏳ 이 작업은 1~2분 정도 소요될 수 있습니다..."
echo ""

if [ ! -f "$DATA_DIR/init-coupon-issue-test.sql" ]; then
    echo "❌ 오류: $DATA_DIR/init-coupon-issue-test.sql 파일을 찾을 수 없습니다!"
    exit 1
fi

if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" < "$DATA_DIR/init-coupon-issue-test.sql"; then
    echo ""
    echo "✅ 테스트 데이터 초기화 완료"
else
    echo "❌ 테스트 데이터 초기화 실패!"
    exit 1
fi
echo ""

# ============================================
# 2단계: K6 부하 테스트 실행
# ============================================

echo "=========================================="
echo "[2/4] K6 쿠폰 발급 부하 테스트 실행 중..."
echo "=========================================="
echo "테스트 스크립트: coupon-issue-basic-test.js"
echo "결과 저장 위치: $K6_RESULT_FILE"
echo ""

TEST_SCRIPT="$SCRIPT_DIR/coupon-issue-basic-test.js"

if [ ! -f "$TEST_SCRIPT" ]; then
    echo "❌ 오류: $TEST_SCRIPT 파일을 찾을 수 없습니다!"
    exit 1
fi

# K6 설치 확인
if ! command -v k6 &> /dev/null; then
    echo "❌ K6가 설치되어 있지 않습니다!"
    echo "설치 방법: https://k6.io/docs/getting-started/installation/"
    exit 1
fi

# K6 테스트 실행 시작 시간 기록
TEST_START_TIME=$(date '+%Y-%m-%d %H:%M:%S')

# Consumer Lag 모니터링 백그라운드 시작
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🔍 Consumer Lag 모니터링 시작"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

monitor_consumer_lag 2 &  # 2초마다 모니터링
LAG_MONITOR_PID=$!

sleep 1  # 모니터링 시작 대기

# K6 실행
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo "🚀 K6 부하 테스트 시작"
echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
echo ""

if k6 run "$TEST_SCRIPT" --out json="$K6_RESULT_FILE"; then
    echo ""
    echo "✅ K6 부하 테스트 완료"
else
    echo ""
    echo "⚠️  K6 부하 테스트가 threshold 실패 또는 오류로 종료되었습니다."
    echo "   계속 진행하여 정합성 검증을 수행합니다..."
fi

TEST_END_TIME=$(date '+%Y-%m-%d %H:%M:%S')
echo ""

# ============================================
# 3단계: 지연 처리 추적
# ============================================

# 지연 처리 추적 함수
track_delayed_processing() {
  local elapsed_time=$1
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')

  echo ""
  echo "=========================================="
  echo "📊 [$timestamp] ${elapsed_time} 경과"
  echo "=========================================="
  echo ""

  # 쿠폰 발급 현황
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "쿠폰 발급 현황"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" -e "
    SELECT
      COUNT(*) AS '총_발급_건수',
      COUNT(DISTINCT user_id) AS '발급_받은_사용자_수',
      COUNT(DISTINCT coupon_id) AS '발급된_쿠폰_종류',
      SUM(CASE WHEN issued_at >= '$TEST_START_TIME' THEN 1 ELSE 0 END) AS '테스트_중_발급_건수'
    FROM user_coupons;
  "
  echo ""

  # Consumer Lag
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "Consumer Lag 현황"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  LAG=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups \
    --bootstrap-server $KAFKA_BOOTSTRAP_SERVER \
    --group $COUPON_CONSUMER_GROUP \
    --describe 2>&1 | \
    awk 'NR > 1 && NF >= 5 { sum += $5 } END { print sum+0 }' 2>/dev/null || echo "0")

  if [ "$LAG" -eq 0 ]; then
    echo -e "  ${GREEN}✅ ${COUPON_CONSUMER_GROUP}: LAG = 0${NC}"
  elif [ "$LAG" -lt 100 ]; then
    echo -e "  ${YELLOW}⚠️  ${COUPON_CONSUMER_GROUP}: LAG = ${LAG}${NC}"
  else
    echo -e "  ${RED}❌ ${COUPON_CONSUMER_GROUP}: LAG = ${LAG}${NC}"
  fi

  echo ""
  echo "총 Lag: ${LAG}"
  echo ""
}

# 지연 처리 추적 로그 파일 초기화
{
  echo "=========================================="
  echo "지연 처리 추적 리포트 - 쿠폰 발급"
  echo "=========================================="
  echo "테스트 시작: $TEST_START_TIME"
  echo "테스트 종료: $TEST_END_TIME"
  echo "=========================================="
  echo ""
} > "$DELAYED_TRACKING_FILE"

echo "=========================================="
echo "[3/4] 지연 처리 추적 시작"
echo "=========================================="
echo "⏳ 최대 10분간 1분, 5분, 10분 시점 추적"
echo "   (비동기 이벤트 처리 완료 모니터링)"
echo ""

# 추적 시점 배열 (초 단위)
TRACKING_INTERVALS=(60 300 600)  # 1분, 5분, 10분
TRACKING_LABELS=("1분" "5분" "10분")

for i in "${!TRACKING_INTERVALS[@]}"; do
  INTERVAL=${TRACKING_INTERVALS[$i]}
  LABEL=${TRACKING_LABELS[$i]}

  # 이전 시점부터 현재 시점까지 대기
  if [ $i -eq 0 ]; then
    WAIT_TIME=$INTERVAL
  else
    PREV_INTERVAL=${TRACKING_INTERVALS[$((i-1))]}
    WAIT_TIME=$((INTERVAL - PREV_INTERVAL))
  fi

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo "⏳ ${LABEL} 시점까지 대기 중..."
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  for ((sec=WAIT_TIME; sec>0; sec--)); do
    echo -ne "\r남은 시간: ${sec}초   "
    sleep 1
  done
  echo -e "\r✅ ${LABEL} 도달               "
  echo ""

  # 현재 시점 추적 및 로그 저장
  track_delayed_processing "${LABEL}" | tee -a "$DELAYED_TRACKING_FILE"

  # Lag이 0이면 조기 종료
  TOTAL_LAG=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups \
    --bootstrap-server $KAFKA_BOOTSTRAP_SERVER \
    --group $COUPON_CONSUMER_GROUP \
    --describe 2>&1 | \
    awk 'NR > 1 && NF >= 5 { sum += $5 } END { print sum+0 }' 2>/dev/null || echo "0")

  if [ "$TOTAL_LAG" -eq 0 ]; then
    echo ""
    echo "=========================================="
    echo "🎉 모든 이벤트 처리 완료!"
    echo "=========================================="
    echo "✅ Consumer Lag = 0"
    echo ""
    {
      echo ""
      echo "=========================================="
      echo "조기 완료: ${LABEL} 시점에 모든 처리 완료"
      echo "=========================================="
      echo "✅ Consumer Lag = 0"
      echo ""
    } >> "$DELAYED_TRACKING_FILE"
    break
  fi
done

echo ""
echo "✅ 지연 처리 추적 완료"
echo "   결과 파일: $DELAYED_TRACKING_FILE"
echo ""

# ============================================
# 4단계: 데이터 정합성 검증
# ============================================

echo "=========================================="
echo "[4/4] 데이터 정합성 검증 중..."
echo "=========================================="
echo "검증 결과 저장 위치: $VALIDATION_RESULT_FILE"
echo ""

if [ ! -f "$VALIDATION_DIR/coupon-issue-integrity-check.sql" ]; then
    echo "❌ 오류: $VALIDATION_DIR/coupon-issue-integrity-check.sql 파일을 찾을 수 없습니다!"
    exit 1
fi

# 정합성 검증 실행 및 결과 저장
if mysql -h"$DB_HOST" -P"$DB_PORT" -u"$DB_USER" -p"$DB_PASSWORD" "$DB_NAME" \
    < "$VALIDATION_DIR/coupon-issue-integrity-check.sql" \
    > "$VALIDATION_RESULT_FILE" 2>&1; then
    echo "✅ 데이터 정합성 검증 완료"
else
    echo "⚠️  정합성 검증 중 일부 오류 발생 (결과 파일 확인 필요)"
fi
echo ""

# ============================================
# 결과 요약 출력
# ============================================

echo "=========================================="
echo "📊 테스트 결과 요약"
echo "=========================================="
echo "시작 시간: $TEST_START_TIME"
echo "종료 시간: $TEST_END_TIME"
echo ""
echo "📁 결과 파일:"
echo "  - K6 결과: $K6_RESULT_FILE"
echo "  - 정합성 검증: $VALIDATION_RESULT_FILE"
echo "  - Consumer Lag: $CONSUMER_LAG_FILE"
echo "  - 지연 처리 추적: $DELAYED_TRACKING_FILE"
echo ""

# Consumer Lag 통계
echo "=========================================="
echo "📊 Consumer Lag 통계"
echo "=========================================="
echo ""

if [ -f "$CONSUMER_LAG_FILE" ]; then
  MAX_LAG=$(grep -A 20 "Consumer Group: $COUPON_CONSUMER_GROUP" "$CONSUMER_LAG_FILE" | \
            awk 'NF >= 5 { if ($5 > max) max = $5 } END { print max+0 }' 2>/dev/null || echo "0")

  AVG_LAG=$(grep -A 20 "Consumer Group: $COUPON_CONSUMER_GROUP" "$CONSUMER_LAG_FILE" | \
            awk 'NF >= 5 { sum += $5; count++ } END { if (count > 0) print int(sum/count); else print 0 }' 2>/dev/null || echo "0")

  if [ "$MAX_LAG" -eq 0 ]; then
    echo -e "${GREEN}✅ ${COUPON_CONSUMER_GROUP}${NC}"
    echo "   최대 Lag: 0 | 평균 Lag: 0 (실시간 처리)"
  elif [ "$MAX_LAG" -lt 100 ]; then
    echo -e "${YELLOW}⚠️  ${COUPON_CONSUMER_GROUP}${NC}"
    echo "   최대 Lag: $MAX_LAG | 평균 Lag: $AVG_LAG (경미한 지연)"
  else
    echo -e "${RED}❌ ${COUPON_CONSUMER_GROUP}${NC}"
    echo "   최대 Lag: $MAX_LAG | 평균 Lag: $AVG_LAG (병목 발생!)"
  fi
else
  echo "⚠️  Consumer Lag 로그 파일을 찾을 수 없습니다."
fi

echo ""

# 검증 결과 미리보기
echo "=========================================="
echo "🔍 정합성 검증 결과 미리보기"
echo "=========================================="
echo ""

if [ -f "$VALIDATION_RESULT_FILE" ]; then
    awk '/최종 검증 요약/,/검증 완료/ {print}' "$VALIDATION_RESULT_FILE" | head -30
    echo ""
else
    echo "⚠️  검증 결과 파일을 찾을 수 없습니다."
fi

# ============================================
# 완료 메시지
# ============================================

echo "=========================================="
echo "✅ 모든 작업 완료!"
echo "=========================================="
echo ""
echo "📋 다음 단계:"
echo "  1. 지연 처리 추적 결과 확인:"
echo "     cat $DELAYED_TRACKING_FILE"
echo ""
echo "  2. 검증 결과 확인:"
echo "     cat $VALIDATION_RESULT_FILE"
echo ""
echo "  3. K6 결과 분석:"
echo "     cat $K6_RESULT_FILE"
echo ""
echo "  4. Consumer Lag 로그 확인:"
echo "     cat $CONSUMER_LAG_FILE"
echo ""
echo "=========================================="
echo "🎉 스크립트 종료"
echo "=========================================="