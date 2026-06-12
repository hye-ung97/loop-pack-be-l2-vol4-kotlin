package com.loopers.domain.coupon

enum class CouponStatus {
    /** 사용 가능 */
    AVAILABLE,

    /** 사용 완료 */
    USED,

    /** 만료 (DB에 저장하지 않고 조회/검증 시점에 파생되는 상태) */
    EXPIRED,
}
