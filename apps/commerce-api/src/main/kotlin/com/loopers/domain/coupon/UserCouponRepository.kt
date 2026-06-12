package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface UserCouponRepository {
    fun save(userCoupon: UserCouponModel): UserCouponModel
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponModel?
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<UserCouponModel>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<UserCouponModel>
}
