#!/bin/bash

# Consumer Lag 모니터링 스크립트
# 부하 테스트 중 Consumer가 메시지를 잘 처리하는지 실시간 확인

# Kafka 컨테이너 이름 (docker-compose.yml 기준)
KAFKA_CONTAINER="ecommerce-kafka"

# 모니터링할 Consumer Groups
CONSUMER_GROUPS=(
  "stock-service-group"
  "coupon-service-group"
  "payment-service-group"
  "order-flow-manager-group"
  "post-process-group"
  "external-data-service-group"
)

# 색상 코드
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

echo "=========================================="
echo "🔍 Kafka Consumer Lag 모니터링"
echo "=========================================="
echo ""

# 무한 루프로 1초마다 모니터링
while true; do
  clear
  echo "=========================================="
  echo "📊 Consumer Lag 모니터링 ($(date '+%Y-%m-%d %H:%M:%S'))"
  echo "=========================================="
  echo ""

  for GROUP in "${CONSUMER_GROUPS[@]}"; do
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
    echo -e "${BLUE}Consumer Group: ${GROUP}${NC}"
    echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"

    # Docker exec로 Kafka 컨테이너 내부에서 명령 실행
    OUTPUT=$(docker exec $KAFKA_CONTAINER kafka-consumer-groups \
      --bootstrap-server localhost:9093 \
      --group $GROUP \
      --describe 2>&1)

    # Consumer Group이 존재하지 않는 경우
    if echo "$OUTPUT" | grep -q "does not exist"; then
      echo -e "${YELLOW}⚠ Consumer Group이 아직 생성되지 않았습니다${NC}"
      echo ""
      continue
    fi

    # Consumer Group이 비어있는 경우
    if echo "$OUTPUT" | grep -q "Consumer group .* has no active members"; then
      echo -e "${YELLOW}⚠ Active Consumer가 없습니다 (애플리케이션 미실행)${NC}"
      echo ""
      continue
    fi

    # 정상 출력
    echo "$OUTPUT" | grep -v "WARN" | awk '
      BEGIN {
        printf "%-30s %-10s %-15s %-15s %-10s\n", "TOPIC", "PARTITION", "CURRENT-OFFSET", "LOG-END-OFFSET", "LAG"
        printf "%-30s %-10s %-15s %-15s %-10s\n", "------------------------------", "----------", "---------------", "---------------", "----------"
      }
      NR > 1 {
        if (NF >= 6) {
          topic = $1
          partition = $2
          current_offset = $3
          log_end_offset = $4
          lag = $5

          # LAG 색상 구분
          lag_color = ""
          if (lag == "0") {
            lag_color = "\033[0;32m" # Green
          } else if (lag < 100) {
            lag_color = "\033[1;33m" # Yellow
          } else {
            lag_color = "\033[0;31m" # Red
          }

          printf "%-30s %-10s %-15s %-15s %s%-10s\033[0m\n", topic, partition, current_offset, log_end_offset, lag_color, lag
        }
      }
    '

    echo ""
  done

  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo -e "${GREEN}💡 해석 가이드:${NC}"
  echo -e "  ${GREEN}LAG = 0${NC}      → 실시간 처리 중 ✅"
  echo -e "  ${YELLOW}LAG < 100${NC}   → 경미한 지연 ⚠️"
  echo -e "  ${RED}LAG >= 100${NC}  → 처리 병목 발생 ❌"
  echo -e "${BLUE}━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━${NC}"
  echo ""
  echo "Ctrl+C로 종료 | 1초마다 자동 갱신"

  sleep 1
done