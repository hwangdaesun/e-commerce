import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Trend } from 'k6/metrics';
import { randomIntBetween } from 'https://jslib.k6.io/k6-utils/1.2.0/index.js';

// ============================================
// 커스텀 메트릭 정의
// ============================================

const successCounter = new Counter('coupon_issue_success');
const failureCounter = new Counter('coupon_issue_failure');
const responseTrend = new Trend('coupon_issue_response_time');

// ============================================
// 테스트 설정
// ============================================

export const options = {
    stages: [
        { duration: '1m', target: 20 },   // Ramp-up: 0 → 20 VUs (준비)
        { duration: '2m', target: 50 },   // Ramp-up: 20 → 50 VUs
        { duration: '2m', target: 100 },  // Ramp-up: 50 → 100 VUs
        { duration: '4m', target: 100 },  // Sustained: 100 VUs 유지
        { duration: '1m', target: 0 },    // Ramp-down: 100 → 0 VUs
    ],
    thresholds: {
        http_req_failed: ['rate<0.05'],        // HTTP 실패율 < 5%
        http_req_duration: ['p(95)<1000'],     // P95 응답 시간 < 1초
        http_req_duration: ['p(99)<2000'],     // P99 응답 시간 < 2초
        coupon_issue_success: ['count>0'],     // 최소 1건 이상 성공
    },
};

// ============================================
// 환경 설정
// ============================================

const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 쿠폰 ID 범위 (미리 생성된 쿠폰 사용)
// 100개의 쿠폰을 미리 생성한다고 가정
const MIN_COUPON_ID = 1;
const MAX_COUPON_ID = 100;

// 사용자 ID 범위 (100만 명)
const MIN_USER_ID = 1;
const MAX_USER_ID = 1000000;

// ============================================
// 메인 테스트 시나리오
// ============================================

export default function () {
    // 랜덤 쿠폰 선택
    const couponId = randomIntBetween(MIN_COUPON_ID, MAX_COUPON_ID);

    // 랜덤 사용자 선택
    const userId = randomIntBetween(MIN_USER_ID, MAX_USER_ID);

    // 쿠폰 발급 요청
    const issueStartTime = Date.now();

    const issuePayload = JSON.stringify({
        userId: userId,
    });

    const issueParams = {
        headers: {
            'Content-Type': 'application/json',
        },
        tags: {
            name: 'IssueCoupon',
        },
    };

    const issueRes = http.post(
        `${BASE_URL}/api/coupons/${couponId}/issue`,
        issuePayload,
        issueParams
    );

    const issueResponseTime = Date.now() - issueStartTime;
    responseTrend.add(issueResponseTime);

    // 응답 검증
    const issueSuccess = check(issueRes, {
        '쿠폰 발급 요청 접수 (202 Accepted)': (r) => r.status === 202,
        '쿠폰 발급 응답 시간 < 3초': (r) => issueResponseTime < 3000,
    });

    if (issueSuccess) {
        successCounter.add(1);
    } else {
        failureCounter.add(1);
        console.error(`쿠폰 발급 실패: couponId=${couponId}, userId=${userId}, status=${issueRes.status}`);
    }

    // Think time (사용자가 다음 동작까지 대기하는 시간)
    sleep(randomIntBetween(1, 3));
}

// ============================================
// Setup: 테스트 시작 전 초기화
// ============================================

export function setup() {
    console.log('========================================');
    console.log('쿠폰 발급 부하 테스트 시작');
    console.log('========================================');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`쿠폰 ID 범위: ${MIN_COUPON_ID} ~ ${MAX_COUPON_ID}`);
    console.log(`사용자 ID 범위: ${MIN_USER_ID} ~ ${MAX_USER_ID}`);
    console.log('========================================');
    console.log('');

    // 헬스체크
    const healthRes = http.get(`${BASE_URL}/actuator/health`);
    if (healthRes.status !== 200) {
        console.error('❌ 서버가 응답하지 않습니다!');
        throw new Error('Server is not healthy');
    }
    console.log('✅ 서버 헬스체크 성공');
    console.log('');

    return { startTime: new Date().toISOString() };
}

// ============================================
// Teardown: 테스트 종료 후 처리
// ============================================

export function teardown(data) {
    console.log('');
    console.log('========================================');
    console.log('쿠폰 발급 부하 테스트 완료');
    console.log('========================================');
    console.log(`시작 시간: ${data.startTime}`);
    console.log(`종료 시간: ${new Date().toISOString()}`);
    console.log('========================================');
    console.log('');
    console.log('📊 다음 단계:');
    console.log('  1. Consumer Lag 확인');
    console.log('  2. 데이터 정합성 검증 (validation SQL 실행)');
    console.log('  3. 발급 성공률 확인');
    console.log('========================================');
}