package com.loopers.domain.coupon

import com.loopers.domain.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import java.time.ZonedDateTime

@Entity
@Table(
    name = "user_coupons",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_user_coupons_user_coupon", columnNames = ["user_id", "coupon_id"]),
    ],
)
class UserCouponModel(
    userId: Long,
    couponId: Long,
) : BaseEntity() {
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Column(name = "coupon_id", nullable = false)
    var couponId: Long = couponId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: CouponStatus = CouponStatus.AVAILABLE
        protected set

    @Column(name = "used_at")
    var usedAt: ZonedDateTime? = null
        protected set

    /**
     * 조회/검증 시점의 상태를 파생한다.
     * 만료(EXPIRED)는 DB에 저장하지 않고, 템플릿의 만료 시각과 현재 시각을 비교해 계산한다.
     */
    fun statusAt(coupon: CouponModel, now: ZonedDateTime): CouponStatus = when {
        status == CouponStatus.USED -> CouponStatus.USED
        coupon.isExpired(now) -> CouponStatus.EXPIRED
        else -> CouponStatus.AVAILABLE
    }
}
