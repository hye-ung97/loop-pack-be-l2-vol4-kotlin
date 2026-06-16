package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface UserCouponRepository {
    fun save(userCoupon: UserCouponModel): UserCouponModel
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean
    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponModel?
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<UserCouponModel>
    fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<UserCouponModel>

    /** AVAILABLE 상태일 때만 원자적으로 USED 로 전이한다. 반환값은 영향받은 행 수(1: 성공, 0: 이미 사용됨). */
    fun useIfAvailable(id: Long, usedAt: ZonedDateTime): Int
}
