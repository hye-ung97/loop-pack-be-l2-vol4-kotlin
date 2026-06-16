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
        if (coupon.isExpired(now)) {
            throw CoreException(ErrorType.CONFLICT, "만료된 쿠폰입니다.")
        }
        val discount = coupon.calculateDiscount(orderAmount)
        // 상태 조건부 UPDATE로 동시 사용 시 단 한 번만 성공하도록 보장한다.
        if (userCouponRepository.useIfAvailable(userCoupon.id, now) == 0) {
            throw CoreException(ErrorType.CONFLICT, "이미 사용된 쿠폰입니다.")
        }
        return discount
    }

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByUserId(userId, pageable)

    fun getIssues(couponId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByCouponId(couponId, pageable)
}
