package com.loopers.infrastructure.coupon

import com.loopers.domain.coupon.CouponModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface CouponJpaRepository : JpaRepository<CouponModel, Long> {
    @Query("SELECT c FROM CouponModel c WHERE c.id = :id AND c.deletedAt IS NULL")
    fun findActiveById(id: Long): CouponModel?

    @Query(
        value = "SELECT c FROM CouponModel c WHERE c.deletedAt IS NULL",
        countQuery = "SELECT COUNT(c) FROM CouponModel c WHERE c.deletedAt IS NULL",
    )
    fun findAllActive(pageable: Pageable): Page<CouponModel>
}
