package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
@Transactional(readOnly = true)
class UserCouponService(
    private val userCouponRepository: UserCouponRepository,
    private val couponRepository: CouponRepository,
) {
    @Transactional
    fun issue(userId: Long, couponId: Long): UserCouponModel {
        couponRepository.findActiveById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다.")
        if (userCouponRepository.existsByUserIdAndCouponId(userId, couponId)) {
            throw CoreException(ErrorType.CONFLICT, "이미 발급받은 쿠폰입니다.")
        }
        return userCouponRepository.save(UserCouponModel(userId = userId, couponId = couponId))
    }

    @Transactional
    fun use(userId: Long, couponId: Long, orderAmount: Long, now: ZonedDateTime): Long {
        val userCoupon = userCouponRepository.findByUserIdAndCouponId(userId, couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "보유하지 않은 쿠폰입니다.")
        val coupon = couponRepository.findActiveById(couponId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 쿠폰입니다.")
        if (userCoupon.statusAt(coupon, now) != CouponStatus.AVAILABLE) {
            throw CoreException(ErrorType.CONFLICT, "사용할 수 없는 쿠폰입니다.")
        }
        val discount = coupon.calculateDiscount(orderAmount)
        userCoupon.use(now)
        return discount
    }

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByUserId(userId, pageable)

    fun getIssues(couponId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByCouponId(couponId, pageable)
}
