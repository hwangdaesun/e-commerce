// K6 부하 테스트 공통 설정

export const BASE_URL = __ENV.BASE_URL || 'http://localhost:8080';

// 테스트 데이터 범위 (init-data.sql 기준)
export const TEST_DATA = {
    USER_ID_MIN: 1,
    USER_ID_MAX: 1000000,        // 100만 사용자
    ITEM_ID_MIN: 1,
    ITEM_ID_MAX: 1000000,        // 100만 상품
    COUPON_ID_MIN: 1,
    COUPON_ID_MAX: 10000,        // 1만개 쿠폰
    CART_ID_MIN: 1,
    CART_ID_MAX: 200000,         // 20만개 장바구니
};

// 성능 임계값 (SLA)
export const THRESHOLDS = {
    // 주문 생성 API
    ORDER_CREATE: {
        http_req_duration: ['p(95)<2000', 'p(99)<3000'],  // P95 < 2s, P99 < 3s
        http_req_failed: ['rate<0.05'],                     // 에러율 < 5%
    },
    // 일반 조회 API
    QUERY: {
        http_req_duration: ['p(95)<500', 'p(99)<1000'],   // P95 < 500ms, P99 < 1s
        http_req_failed: ['rate<0.01'],                     // 에러율 < 1%
    },
};

// 부하 패턴
export const LOAD_PATTERNS = {
    // 점진적 증가 (Ramp-up)
    RAMP_UP: {
        stages: [
            { duration: '2m', target: 50 },      // 0 → 50명 (2분)
            { duration: '3m', target: 100 },     // 50 → 100명 (3분)
            { duration: '5m', target: 100 },     // 100명 유지 (5분)
            { duration: '2m', target: 0 },       // 100 → 0명 (2분)
        ],
    },

    // 스파이크 테스트
    SPIKE: {
        stages: [
            { duration: '30s', target: 10 },     // 준비
            { duration: '1m', target: 500 },     // 급격한 증가
            { duration: '2m', target: 500 },     // 유지
            { duration: '30s', target: 0 },      // 급격한 감소
        ],
    },

    // 스트레스 테스트 (지속적 고부하)
    STRESS: {
        stages: [
            { duration: '2m', target: 100 },     // 워밍업
            { duration: '5m', target: 200 },     // 200명 유지
            { duration: '5m', target: 300 },     // 300명 유지
            { duration: '5m', target: 400 },     // 400명 유지
            { duration: '2m', target: 0 },       // 종료
        ],
    },

    // 내구성 테스트 (장시간 일정 부하)
    SOAK: {
        stages: [
            { duration: '5m', target: 100 },     // 워밍업
            { duration: '1h', target: 100 },     // 1시간 유지
            { duration: '5m', target: 0 },       // 종료
        ],
    },
};

// HTTP 설정
export const HTTP_CONFIG = {
    headers: {
        'Content-Type': 'application/json',
    },
    timeout: '30s',
};