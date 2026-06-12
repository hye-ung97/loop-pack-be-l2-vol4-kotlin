package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import java.time.ZonedDateTime

@Entity
@Table(name = "coupons")
class CouponModel(
    name: String,
    discountType: DiscountType,
    discountValue: Long,
    minOrderAmount: Long?,
    expiredAt: ZonedDateTime,
) : BaseEntity() {
    @Column(name = "name", nullable = false)
    var name: String = name
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "discount_type", nullable = false)
    var discountType: DiscountType = discountType
        protected set

    @Column(name = "discount_value", nullable = false)
    var discountValue: Long = discountValue
        protected set

    @Column(name = "min_order_amount")
    var minOrderAmount: Long? = minOrderAmount
        protected set

    @Column(name = "expired_at", nullable = false)
    var expiredAt: ZonedDateTime = expiredAt
        protected set

    init {
        requireValidName(name)
        discountType.validateValue(discountValue)
        if (minOrderAmount != null && minOrderAmount < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "최소 주문 금액은 0 이상이어야 합니다.")
        }
    }

    fun calculateDiscount(orderAmount: Long): Long {
        minOrderAmount?.let {
            if (orderAmount < it) {
                throw CoreException(ErrorType.CONFLICT, "최소 주문 금액 조건을 만족하지 않습니다.")
            }
        }
        return discountType.calculate(discountValue, orderAmount)
    }

    fun isExpired(now: ZonedDateTime): Boolean = now.isAfter(expiredAt)

    private fun requireValidName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "쿠폰 이름은 비어있을 수 없습니다.")
        }
    }
}
