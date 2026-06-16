package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface UserCouponJpaRepository : JpaRepository<UserCouponModel, Long> {
    fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean

    fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponModel?

    fun findAllByUserIdOrderByIdDesc(userId: Long, pageable: Pageable): Page<UserCouponModel>

    fun findAllByCouponIdOrderByIdDesc(couponId: Long, pageable: Pageable): Page<UserCouponModel>

    @Modifying
    @Query(
        """
            UPDATE UserCouponModel uc
            SET uc.status = com.loopers.domain.coupon.CouponStatus.USED, uc.usedAt = :usedAt
            WHERE uc.id = :id AND uc.status = com.loopers.domain.coupon.CouponStatus.AVAILABLE
        """,
    )
    fun useIfAvailable(@Param("id") id: Long, @Param("usedAt") usedAt: ZonedDateTime): Int
}
