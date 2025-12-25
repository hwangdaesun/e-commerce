import { TEST_DATA } from './config.js';

/**
 * 랜덤 정수 생성 (min <= n <= max)
 */
export function randomInt(min, max) {
    return Math.floor(Math.random() * (max - min + 1)) + min;
}

/**
 * 랜덤 사용자 ID 생성
 */
export function randomUserId() {
    return randomInt(TEST_DATA.USER_ID_MIN, TEST_DATA.USER_ID_MAX);
}

/**
 * 랜덤 상품 ID 생성
 */
export function randomItemId() {
    return randomInt(TEST_DATA.ITEM_ID_MIN, TEST_DATA.ITEM_ID_MAX);
}

/**
 * 랜덤 쿠폰 ID 생성
 */
export function randomCouponId() {
    return randomInt(TEST_DATA.COUPON_ID_MIN, TEST_DATA.COUPON_ID_MAX);
}

/**
 * 특정 범위의 사용자 ID 생성 (동시성 테스트용)
 */
export function concurrentUserId(startId, count) {
    return startId + (__VU - 1) % count;
}

/**
 * 특정 상품 ID 배열 생성 (재고 경합 테스트용)
 */
export function hotItemIds(itemId, count = 1) {
    const items = [];
    for (let i = 0; i < count; i++) {
        items.push(itemId + i);
    }
    return items;
}

/**
 * 랜덤 장바구니 아이템 생성 (1~5개)
 */
export function generateRandomCartItems(count = null) {
    const itemCount = count || randomInt(1, 5);
    const items = [];

    for (let i = 0; i < itemCount; i++) {
        items.push({
            itemId: randomItemId(),
            quantity: randomInt(1, 3),
        });
    }

    return items;
}

/**
 * 주문 요청 페이로드 생성
 */
export function createOrderPayload(userId, cartItemIds, userCouponId = null) {
    const payload = {
        userId: userId,
        cartItemIds: cartItemIds,
    };

    if (userCouponId !== null) {
        payload.userCouponId = userCouponId;
    }

    return JSON.stringify(payload);
}

/**
 * 응답 검증 헬퍼
 */
export function validateOrderResponse(response) {
    if (response.status !== 201) {
        return { valid: false, error: `Expected 201, got ${response.status}` };
    }

    const body = JSON.parse(response.body);

    if (!body.orderId) {
        return { valid: false, error: 'Missing orderId in response' };
    }

    if (!Array.isArray(body.orderItems) || body.orderItems.length === 0) {
        return { valid: false, error: 'Invalid orderItems in response' };
    }

    if (typeof body.totalAmount !== 'number' || body.totalAmount <= 0) {
        return { valid: false, error: 'Invalid totalAmount in response' };
    }

    return { valid: true, data: body };
}

/**
 * 성능 메트릭 로깅
 */
export function logMetrics(name, response, startTime) {
    const duration = Date.now() - startTime;
    console.log(`[${name}] Status: ${response.status}, Duration: ${duration}ms`);
}

/**
 * 에러 로깅
 */
export function logError(scenario, error, response = null) {
    const msg = `[ERROR] ${scenario}: ${error}`;
    if (response) {
        console.log(`${msg} | Status: ${response.status} | Body: ${response.body}`);
    } else {
        console.log(msg);
    }
}

/**
 * 배열에서 랜덤 요소 선택
 */
export function randomChoice(array) {
    return array[Math.floor(Math.random() * array.length)];
}

/**
 * Sleep 헬퍼 (ms)
 */
export function sleep(ms) {
    const start = Date.now();
    while (Date.now() - start < ms);
}