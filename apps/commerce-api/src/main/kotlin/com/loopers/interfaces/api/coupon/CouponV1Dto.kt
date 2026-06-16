package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.MyCouponInfo
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import java.time.ZonedDateTime

class CouponV1Dto {
    data class MyCouponResponse(
        val id: Long,
        val couponId: Long,
        val name: String,
        val type: DiscountType,
        val value: Long,
        val minOrderAmount: Long?,
        val status: CouponStatus,
        val expiredAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: MyCouponInfo): MyCouponResponse = MyCouponResponse(
                id = info.id,
                couponId = info.couponId,
                name = info.name,
                type = info.discountType,
                value = info.discountValue,
                minOrderAmount = info.minOrderAmount,
                status = info.status,
                expiredAt = info.expiredAt,
                usedAt = info.usedAt,
            )
        }
    }

    data class MyCouponPageResponse(
        val content: List<MyCouponResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
