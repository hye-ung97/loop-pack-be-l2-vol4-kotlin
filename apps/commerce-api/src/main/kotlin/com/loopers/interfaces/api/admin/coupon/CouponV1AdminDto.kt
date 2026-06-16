package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponInfo
import com.loopers.application.coupon.IssuedCouponInfo
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import java.time.LocalDateTime
import java.time.ZonedDateTime

class CouponV1AdminDto {
    data class CreateRequest(
        val name: String,
        val type: DiscountType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: LocalDateTime,
    )

    data class UpdateRequest(
        val name: String,
        val type: DiscountType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: LocalDateTime,
    )

    data class CouponResponse(
        val id: Long,
        val name: String,
        val type: DiscountType,
        val value: Long,
        val minOrderAmount: Long?,
        val expiredAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: CouponInfo): CouponResponse = CouponResponse(
                id = info.id,
                name = info.name,
                type = info.discountType,
                value = info.discountValue,
                minOrderAmount = info.minOrderAmount,
                expiredAt = info.expiredAt,
            )
        }
    }

    data class CouponPageResponse(
        val content: List<CouponResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )

    data class IssueResponse(
        val id: Long,
        val userId: Long,
        val couponId: Long,
        val status: CouponStatus,
        val issuedAt: ZonedDateTime,
        val usedAt: ZonedDateTime?,
    ) {
        companion object {
            fun from(info: IssuedCouponInfo): IssueResponse = IssueResponse(
                id = info.id,
                userId = info.userId,
                couponId = info.couponId,
                status = info.status,
                issuedAt = info.issuedAt,
                usedAt = info.usedAt,
            )
        }
    }

    data class IssuePageResponse(
        val content: List<IssueResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
