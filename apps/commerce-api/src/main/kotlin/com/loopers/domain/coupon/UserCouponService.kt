package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

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

    fun getMyCoupons(userId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByUserId(userId, pageable)

    fun getIssues(couponId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponRepository.findAllByCouponId(couponId, pageable)
}
