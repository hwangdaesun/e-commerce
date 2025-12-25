#!/bin/bash

# ============================================
# K6 부하 테스트용 초기 데이터 생성 실행 스크립트
# ============================================

set -e  # 에러 발생시 즉시 종료

# 색상 정의
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# 스크립트 시작
echo -e "${BLUE}========================================${NC}"
echo -e "${BLUE}K6 부하 테스트 초기 데이터 생성${NC}"
echo -e "${BLUE}========================================${NC}"
echo ""

# MySQL 접속 정보 입력
read -p "MySQL 사용자명 [root]: " DB_USER
DB_USER=${DB_USER:-root}

read -sp "MySQL 비밀번호: " DB_PASSWORD
echo ""

read -p "데이터베이스명 [ecommerce]: " DB_NAME
DB_NAME=${DB_NAME:-ecommerce}

read -p "MySQL 호스트 [localhost]: " DB_HOST
DB_HOST=${DB_HOST:-localhost}

read -p "MySQL 포트 [3306]: " DB_PORT
DB_PORT=${DB_PORT:-3306}

echo ""
echo -e "${YELLOW}[확인] 다음 설정으로 진행합니다:${NC}"
echo "  - 호스트: ${DB_HOST}:${DB_PORT}"
echo "  - 사용자: ${DB_USER}"
echo "  - 데이터베이스: ${DB_NAME}"
echo ""

# 데이터베이스 연결 테스트
echo -e "${BLUE}[1/4] 데이터베이스 연결 확인 중...${NC}"
if ! mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" -e "USE ${DB_NAME};" 2>/dev/null; then
    echo -e "${RED}❌ 데이터베이스 연결 실패${NC}"
    echo -e "${RED}데이터베이스가 존재하는지, 접속 정보가 올바른지 확인하세요.${NC}"
    exit 1
fi
echo -e "${GREEN}✅ 데이터베이스 연결 성공${NC}"
echo ""

# 디스크 공간 확인
echo -e "${BLUE}[2/4] 디스크 공간 확인 중...${NC}"
if [[ "$OSTYPE" == "darwin"* ]]; then
    # macOS
    AVAILABLE_SPACE=$(df -g . | awk 'NR==2 {print $4}')
    echo "  사용 가능한 공간: ${AVAILABLE_SPACE}GB"
    if [ "$AVAILABLE_SPACE" -lt 10 ]; then
        echo -e "${YELLOW}⚠️  경고: 디스크 공간이 부족할 수 있습니다. (최소 10GB 권장)${NC}"
        read -p "계속 진행하시겠습니까? (y/N): " CONTINUE
        if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
            echo -e "${RED}중단되었습니다.${NC}"
            exit 1
        fi
    fi
else
    # Linux
    AVAILABLE_SPACE=$(df -BG . | awk 'NR==2 {print $4}' | sed 's/G//')
    echo "  사용 가능한 공간: ${AVAILABLE_SPACE}GB"
    if [ "$AVAILABLE_SPACE" -lt 10 ]; then
        echo -e "${YELLOW}⚠️  경고: 디스크 공간이 부족할 수 있습니다. (최소 10GB 권장)${NC}"
        read -p "계속 진행하시겠습니까? (y/N): " CONTINUE
        if [[ ! "$CONTINUE" =~ ^[Yy]$ ]]; then
            echo -e "${RED}중단되었습니다.${NC}"
            exit 1
        fi
    fi
fi
echo -e "${GREEN}✅ 디스크 공간 확인 완료${NC}"
echo ""

# 백업 권장
echo -e "${BLUE}[3/4] 데이터 백업 (선택사항)${NC}"
echo -e "${YELLOW}기존 데이터가 있다면 백업을 권장합니다.${NC}"
read -p "백업을 생성하시겠습니까? (y/N): " CREATE_BACKUP
if [[ "$CREATE_BACKUP" =~ ^[Yy]$ ]]; then
    BACKUP_FILE="backup_${DB_NAME}_$(date +%Y%m%d_%H%M%S).sql"
    echo "  백업 파일명: ${BACKUP_FILE}"
    echo "  백업 중..."
    if mysqldump -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" > "${BACKUP_FILE}" 2>/dev/null; then
        echo -e "${GREEN}✅ 백업 완료: ${BACKUP_FILE}${NC}"
    else
        echo -e "${RED}❌ 백업 실패${NC}"
        exit 1
    fi
else
    echo "  백업을 건너뜁니다."
fi
echo ""

# 초기 데이터 생성
echo -e "${BLUE}[4/4] 초기 데이터 생성 시작${NC}"
echo -e "${YELLOW}예상 소요 시간: 약 5~10분${NC}"
echo ""

START_TIME=$(date +%s)

if mysql -h"${DB_HOST}" -P"${DB_PORT}" -u"${DB_USER}" -p"${DB_PASSWORD}" "${DB_NAME}" < init.sql 2>&1; then
    END_TIME=$(date +%s)
    DURATION=$((END_TIME - START_TIME))
    MINUTES=$((DURATION / 60))
    SECONDS=$((DURATION % 60))

    echo ""
    echo -e "${GREEN}========================================${NC}"
    echo -e "${GREEN}✅ 초기 데이터 생성 완료!${NC}"
    echo -e "${GREEN}========================================${NC}"
    echo -e "소요 시간: ${MINUTES}분 ${SECONDS}초"
    echo ""
    echo -e "${BLUE}다음 단계:${NC}"
    echo "1. 데이터 검증:"
    echo "   mysql -u ${DB_USER} -p ${DB_NAME}"
    echo "   SELECT COUNT(*) FROM users;"
    echo ""
    echo "2. 추가 테스트 데이터 준비:"
    echo "   cd .."
    echo "   mysql -u ${DB_USER} -p ${DB_NAME} < prepare-test-data.sql"
    echo ""
    echo "3. K6 부하 테스트 실행:"
    echo "   cd .."
    echo "   k6 run order-basic-test.js"
    echo ""
else
    echo -e "${RED}❌ 데이터 생성 실패${NC}"
    echo -e "${RED}오류 로그를 확인하세요.${NC}"
    exit 1
fi