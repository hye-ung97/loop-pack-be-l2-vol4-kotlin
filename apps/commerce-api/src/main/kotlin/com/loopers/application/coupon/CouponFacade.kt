package com.loopers.application.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.user.UserService
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageImpl
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import java.time.ZonedDateTime

@Component
class CouponFacade(
    private val userService: UserService,
    private val couponService: CouponService,
    private val userCouponService: UserCouponService,
) {
    fun issue(loginId: String, rawPassword: String, couponId: Long): MyCouponInfo {
        val user = userService.authenticate(loginId, rawPassword)
        val userCoupon = userCouponService.issue(user.id, couponId)
        val coupon = couponService.getById(couponId)
        return MyCouponInfo.from(userCoupon, coupon, ZonedDateTime.now())
    }

    fun getMyCoupons(loginId: String, rawPassword: String, pageable: Pageable): Page<MyCouponInfo> {
        val user = userService.authenticate(loginId, rawPassword)
        val page = userCouponService.getMyCoupons(user.id, pageable)
        val couponsById = couponService.findAllByIds(page.content.map { it.couponId }.distinct()).associateBy { it.id }
        val now = ZonedDateTime.now()
        val content = page.content.mapNotNull { userCoupon ->
            val coupon = couponsById[userCoupon.couponId] ?: return@mapNotNull null
            MyCouponInfo.from(userCoupon, coupon, now)
        }
        return PageImpl(content, pageable, page.totalElements)
    }
}
