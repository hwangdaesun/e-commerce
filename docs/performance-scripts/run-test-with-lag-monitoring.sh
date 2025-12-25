#!/bin/bash

# 부하 테스트 + Consumer Lag 모니터링 통합 스크립트
# K6 테스트를 백그라운드에서 실행하면서 실시간으로 Consumer Lag을 모니터링

set -e

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
NC='\033[0m'

# 설정
KAFKA_CONTAINER="ecommerce-kafka"
RESULTS_DIR="results"
TIMESTAMP=$(date +"%Y%m%d_%H%M%S")

# 사용법 출력
usage() {
  echo "사용법: $0 <test-type> [monitoring-interval]"
  echo ""
  echo "test-type:"
  echo "  basic              - 기본 부하 테스트 (12분)"
  echo "  stress             - 스트레스 테스트 (19분)"
  echo "  spike              - 스파이크 테스트 (5분)"
  echo "  stock-contention   - 재고 경합 테스트"
  echo ""
  echo "monitoring-interval: Consumer Lag 체크 주기(초), 기본값 2"
  echo ""
  echo "예시:"
  echo "  $0 basic           # 기본 테스트 + 2초마다 모니터링"
  echo "  $0 stress 5        # 스트레스 테스트 + 5초마다 모니터링"
  exit 1
}

# 인자 확인
if [ $# -lt 1 ]; then
  usage
fi

TEST_TYPE=$1
INTERVAL=${2:-2}  # 기본값 2초

# 테스트 파일 매핑
case $TEST_TYPE in
  basic)
    TEST_FILE="order-basic-test.js"
    TEST_DURATION="12분"
    ;;
  stress)
    TEST_FILE="order-stress-test.js"
    TEST_DURATION="19분"
    ;;
  spike)
    TEST_FILE="order-spike-test.js"
    TEST_DURATION="5분"
    ;;
  stock-contention)
    TEST_FILE="order-stock-contention-test.js"
    TEST_DURATION="3분"
    ;;
  *)
    echo -e "${RED}❌ 알 수 없는 테스트 타입: $TEST_TYPE${NC}"
    usage
    ;;
esac

# 결과 디렉토리 생성
mkdir -p "$RESULTS_DIR"

LAG_LOG_FILE="$RESULTS_DIR/consumer-lag-${TEST_TYPE}-${TIMESTAMP}.log"
K6_RESULT_FILE="$RESULTS_DIR/k6-${TEST_TYPE}-${TIMESTAMP}.json"

echo ""
echo "=========================================="
echo -e "${CYAN}🚀 부하 테스트 + Consumer Lag 모니터링${NC}"
echo "=========================================="
echo -e "${BLUE}테스트 타입:${NC} $TEST_TYPE"
echo -e "${BLUE}예상 소요 시간:${NC} $TEST_DURATION"
echo -e "${BLUE}모니터링 주기:${NC} ${INTERVAL}초"
echo -e "${BLUE}K6 결과 파일:${NC} $K6_RESULT_FILE"
echo -e "${BLUE}Lag 로그 파일:${NC} $LAG_LOG_FILE"
echo "=========================================="
echo ""

# Consumer Groups
CONSUMER_GROUPS=(
  "stock-service-group"
  "coupon-service-group"
  "payment-service-group"
  "order-flow-manager-group"
  "post-process-group"
  "external-data-service-group"
)

# Consumer Lag 수집 함수
collect_lag() {
  local timestamp=$(date '+%Y-%m-%d %H:%M:%S')
  echo "[$timestamp]" >> "$LAG_LOG_FILE"

  for GROUP in "${CONSUMER_GROUPS[@]}"; do
    echo "Consumer Group: $GROUP" >> "$LAG_LOG_FILE"

    docker exec $KAFKA_CONTAINER kafka-consumer-groups \
      --bootstrap-server localhost:9093 \
      --group $GROUP \
      --describe 2>&1 | grep -v "WARN" >> "$LAG_LOG_FILE" || true

    echo "" >> "$LAG_LOG_FILE"
  done

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━" >> "$LAG_LOG_FILE"
  echo "" >> "$LAG_LOG_FILE"
}

# 실시간 Lag 표시 함수
show_lag_summary() {
  echo ""
  echo -e "${YELLOW}📊 현재 Consumer Lag 요약${NC}"

  for GROUP in "${CONSUMER_GROUPS[@]}"; do
    TOTAL_LAG=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups \
      --bootstrap-server localhost:9093 \
      --group $GROUP \
      --describe 2>&1 | \
      awk 'NR > 1 && NF >= 5 { sum += $5 } END { print sum+0 }')

    if [ "$TOTAL_LAG" -eq 0 ]; then
      echo -e "  ${GREEN}✅ $GROUP: LAG = 0 (실시간 처리 중)${NC}"
    elif [ "$TOTAL_LAG" -lt 100 ]; then
      echo -e "  ${YELLOW}⚠️  $GROUP: LAG = $TOTAL_LAG (경미한 지연)${NC}"
    else
      echo -e "  ${RED}❌ $GROUP: LAG = $TOTAL_LAG (처리 병목!)${NC}"
    fi
  done

  echo ""
}

# Lag 모니터링 백그라운드 프로세스
monitor_lag() {
  echo -e "${BLUE}🔍 Consumer Lag 모니터링 시작 (${INTERVAL}초 주기)${NC}"
  echo ""

  while true; do
    collect_lag
    show_lag_summary
    sleep $INTERVAL
  done
}

# 종료 시 정리
cleanup() {
  echo ""
  echo -e "${YELLOW}🛑 테스트 종료 중...${NC}"

  # 백그라운드 프로세스 종료
  jobs -p | xargs -r kill 2>/dev/null || true

  echo ""
  echo "=========================================="
  echo -e "${GREEN}✅ 부하 테스트 완료${NC}"
  echo "=========================================="
  echo -e "${BLUE}K6 결과:${NC} $K6_RESULT_FILE"
  echo -e "${BLUE}Consumer Lag 로그:${NC} $LAG_LOG_FILE"
  echo ""

  # 최종 Lag 통계
  echo -e "${CYAN}📊 최종 Consumer Lag 분석${NC}"
  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"

  for GROUP in "${CONSUMER_GROUPS[@]}"; do
    MAX_LAG=$(grep -A 20 "Consumer Group: $GROUP" "$LAG_LOG_FILE" | \
              awk 'NF >= 5 { if ($5 > max) max = $5 } END { print max+0 }')

    echo -e "${BLUE}$GROUP:${NC} 최대 Lag = $MAX_LAG"
  done

  echo "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━"
  echo ""
  echo -e "${GREEN}💡 Lag 로그 확인:${NC} cat $LAG_LOG_FILE"
  echo ""

  exit 0
}

trap cleanup EXIT INT TERM

# Lag 모니터링 시작 (백그라운드)
monitor_lag &
MONITOR_PID=$!

# 잠시 대기 후 K6 테스트 시작
sleep 2

echo -e "${BLUE}🚀 K6 부하 테스트 시작${NC}"
echo ""

# K6 실행
k6 run "$TEST_FILE" --out json="$K6_RESULT_FILE"

# K6 완료 후 30초간 추가 모니터링 (이벤트 처리 대기)
echo ""
echo -e "${YELLOW}⏳ 이벤트 처리 대기 중 (30초)...${NC}"
echo ""

sleep 30

# cleanup 함수가 자동 실행됨