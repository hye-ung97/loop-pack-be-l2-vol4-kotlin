package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponModel
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCouponModel
import java.time.ZonedDateTime

/** 쿠폰 템플릿 표현 (ADMIN) */
data class CouponInfo(
    val id: Long,
    val name: String,
    val discountType: DiscountType,
    val discountValue: Long,
    val minOrderAmount: Long?,
    val expiredAt: ZonedDateTime,
) {
    companion object {
        fun from(coupon: CouponModel): CouponInfo = CouponInfo(
            id = coupon.id,
            name = coupon.name,
            discountType = coupon.discountType,
            discountValue = coupon.discountValue,
            minOrderAmount = coupon.minOrderAmount,
            expiredAt = coupon.expiredAt,
        )
    }
}

/** 발급 내역 표현 (ADMIN) */
data class IssuedCouponInfo(
    val id: Long,
    val userId: Long,
    val couponId: Long,
    val status: CouponStatus,
    val issuedAt: ZonedDateTime,
    val usedAt: ZonedDateTime?,
) {
    companion object {
        fun from(userCoupon: UserCouponModel, coupon: CouponModel, now: ZonedDateTime): IssuedCouponInfo = IssuedCouponInfo(
            id = userCoupon.id,
            userId = userCoupon.userId,
            couponId = userCoupon.couponId,
            status = userCoupon.statusAt(coupon, now),
            issuedAt = userCoupon.createdAt,
            usedAt = userCoupon.usedAt,
        )
    }
}
