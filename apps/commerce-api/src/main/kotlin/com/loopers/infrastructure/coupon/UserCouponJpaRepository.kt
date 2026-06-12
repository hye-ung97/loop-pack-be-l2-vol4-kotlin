package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository

interface UserCouponJpaRepository : JpaRepository<UserCouponModel, Long> {
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponModel?

    fun findAllByUserIdOrderByIdDesc(userId: Long, pageable: Pageable): Page<UserCouponModel>

    fun findAllByCouponIdOrderByIdDesc(couponId: Long, pageable: Pageable): Page<UserCouponModel>
}
