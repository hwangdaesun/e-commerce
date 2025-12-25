/**
 * 주문 생성 API 부하 테스트 (100 TPS)
 *
 * 목적: 주문 생성 API의 처리량(TPS) 측정 및 성능 분석
 * 시나리오: 초당 100 건의 주문 요청을 처리하는 상황 시뮬레이션
 *
 * 부하 패턴 (TPS 기반):
 *   - 0 → 20 TPS (1분)
 *   - 20 → 50 TPS (2분)
 *   - 50 → 100 TPS (2분)
 *   - 100 TPS 유지 (4분)
 *   - 100 → 0 TPS (1분)
 *   총 소요 시간: 10분
 *
 * 실행 방법:
 * k6 run order-basic-test.js
 */

import http from 'k6/http';
import { check } from 'k6';
import { Rate, Trend } from 'k6/metrics';
import { htmlReport } from "https://raw.githubusercontent.com/benc-uk/k6-reporter/main/dist/bundle.js";
import { textSummary } from "https://jslib.k6.io/k6-summary/0.0.1/index.js";
import { BASE_URL, THRESHOLDS, HTTP_CONFIG, LOAD_PATTERNS } from './config.js';
import {
    randomUserId,
    randomItemId,
    createOrderPayload,
    validateOrderResponse,
    logError,
    randomInt
} from './utils.js';

// 커스텀 메트릭
const orderCreationSuccess = new Rate('order_creation_success');
const orderCreationDuration = new Trend('order_creation_duration');

// 테스트 설정 (TPS 기반)
export const options = {
    scenarios: {
        order_load_test: {
            executor: 'ramping-arrival-rate',
            startRate: 0,
            timeUnit: '1s',
            preAllocatedVUs: 10,      // 사전 할당 VUs
            maxVUs: 200,              // 최대 VUs
            stages: [
                { duration: '1m', target: 20 },      // 0 → 20 TPS (1분)
                { duration: '2m', target: 50 },      // 20 → 50 TPS (2분)
                { duration: '2m', target: 100 },     // 50 → 100 TPS (2분)
                { duration: '4m', target: 100 },     // 100 TPS 유지 (4분)
                { duration: '1m', target: 0 },       // 100 → 0 TPS (1분)
            ],
        },
    },
    thresholds: {
        'http_req_duration': ['p(95)<1000', 'p(99)<2000'],  // 100 TPS 임계값
        'http_req_failed': ['rate<0.05'],  // 에러율 < 5%
        'order_creation_success': ['rate>0.95'],  // 95% 이상 성공
    },
};

// 시나리오 설정 함수
export function setup() {
    console.log('=== 주문 생성 TPS 부하 테스트 시작 ===');
    console.log(`Base URL: ${BASE_URL}`);
    console.log(`Pattern: TPS-based Load (0→100 TPS)`);
    console.log('⚠️  WARNING: 이 테스트는 부하를 생성합니다!');
    console.log('예상 소요 시간: 약 10분');
    return {};
}

// 메인 테스트 함수
export default function () {
    // 1. 랜덤 사용자 선택 (장바구니가 있는 사용자: 1~200,000)
    // init.sql에서 user 1~200,000이 각각 cart를 가지고 있음
    const userId = randomInt(1, 200000);

    // 2. 해당 사용자의 장바구니 아이템 ID 계산
    // init.sql 구조:
    //   - userId N → cartId N (1:1 매핑)
    //   - cartId N → cart_item_ids: (N-1)*3+1, (N-1)*3+2, (N-1)*3+3
    const cartId = userId;  // userId = cartId (1:1 매핑)
    const baseCartItemId = (cartId - 1) * 3 + 1;

    // 1~3개의 장바구니 아이템을 랜덤하게 선택
    const cartItemCount = randomInt(1, 3);
    const cartItemIds = [];
    for (let i = 0; i < cartItemCount; i++) {
        cartItemIds.push(baseCartItemId + i);
    }

    // 3. 주문 생성 요청
    const payload = createOrderPayload(userId, cartItemIds);

    const startTime = Date.now();
    const response = http.post(
        `${BASE_URL}/api/orders`,
        payload,
        { headers: HTTP_CONFIG.headers }
    );
    const duration = Date.now() - startTime;

    // 4. 응답 검증
    const validation = validateOrderResponse(response);

    const success = check(response, {
        '주문 생성 성공 (201)': (r) => r.status === 201,
        '응답 body에 orderId 존재': (r) => {
            try {
                const body = JSON.parse(r.body);
                return body.orderId !== undefined;
            } catch (e) {
                return false;
            }
        },
        '응답 시간 < 3초': (r) => r.timings.duration < 3000,
    });

    // 5. 메트릭 기록
    orderCreationSuccess.add(success);
    orderCreationDuration.add(duration);

    // 6. 에러 로깅
    if (!success) {
        logError('Order Creation', validation.error || 'Unknown error', response);
    }

    // TPS 모드에서는 sleep 제거 (정확한 TPS 유지를 위해)
}

// 테스트 종료 후 실행
export function teardown(data) {
    console.log('=== 주문 생성 TPS 부하 테스트 종료 ===');
    console.log('');
    console.log('📊 분석 포인트:');
    console.log('1. 시스템이 100 TPS를 견뎌냈는가?');
    console.log('2. 어느 TPS 수준에서 성능 저하가 시작되었는가?');
    console.log('3. 에러율과 응답 시간 추이 분석');
    console.log('4. 시스템 리소스 사용률 (CPU, 메모리, DB 커넥션, ThreadPool)');
    console.log('5. 실제 달성한 TPS vs 목표 TPS 비교');
}

// HTML 리포트 생성
export function handleSummary(data) {
    return {
        "order-basic-test-summary.html": htmlReport(data),
        stdout: textSummary(data, { indent: " ", enableColors: true }),
    };
}