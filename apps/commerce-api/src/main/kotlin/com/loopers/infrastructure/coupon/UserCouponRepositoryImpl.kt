package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.UserCouponModel
import com.loopers.domain.coupon.UserCouponRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class UserCouponRepositoryImpl(
    private val userCouponJpaRepository: UserCouponJpaRepository,
) : UserCouponRepository {
    override fun save(userCoupon: UserCouponModel): UserCouponModel = userCouponJpaRepository.save(userCoupon)

    override fun existsByUserIdAndCouponId(userId: Long, couponId: Long): Boolean =
        userCouponJpaRepository.existsByUserIdAndCouponId(userId, couponId)

    override fun findByUserIdAndCouponId(userId: Long, couponId: Long): UserCouponModel? =
        userCouponJpaRepository.findByUserIdAndCouponId(userId, couponId)

    override fun findAllByUserId(userId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponJpaRepository.findAllByUserIdOrderByIdDesc(userId, pageable)

    override fun findAllByCouponId(couponId: Long, pageable: Pageable): Page<UserCouponModel> =
        userCouponJpaRepository.findAllByCouponIdOrderByIdDesc(couponId, pageable)

    override fun useIfAvailable(id: Long, usedAt: ZonedDateTime): Int =
        userCouponJpaRepository.useIfAvailable(id, usedAt)
}
