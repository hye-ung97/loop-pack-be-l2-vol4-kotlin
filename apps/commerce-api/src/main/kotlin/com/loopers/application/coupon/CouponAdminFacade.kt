package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCouponService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponAdminFacade(
    private val couponService: CouponService,
    private val userCouponService: UserCouponService,
) {
    fun create(
        name: String,
        discountType: DiscountType,
        discountValue: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponInfo = CouponInfo.from(couponService.register(name, discountType, discountValue, minOrderAmount, expiredAt))

    fun getById(id: Long): CouponInfo = CouponInfo.from(couponService.getById(id))

    fun getAll(pageable: Pageable): Page<CouponInfo> = couponService.getAll(pageable).map(CouponInfo::from)

    fun update(
        id: Long,
        name: String,
        discountType: DiscountType,
        discountValue: Long,
        minOrderAmount: Long?,
        expiredAt: ZonedDateTime,
    ): CouponInfo {
        couponService.update(id, name, discountType, discountValue, minOrderAmount, expiredAt)
        return CouponInfo.from(couponService.getById(id))
    }

    fun delete(id: Long) {
        couponService.delete(id)
    }

    fun getIssues(couponId: Long, pageable: Pageable): Page<IssuedCouponInfo> {
        val coupon = couponService.getById(couponId)
        val now = ZonedDateTime.now()
        return userCouponService.getIssues(couponId, pageable).map { IssuedCouponInfo.from(it, coupon, now) }
    }
}
