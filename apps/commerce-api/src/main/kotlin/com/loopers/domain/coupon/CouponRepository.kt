package com.loopers.domain.coupon

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface CouponRepository {
    fun save(coupon: CouponModel): CouponModel
    fun findActiveById(id: Long): CouponModel?
    fun findAllActive(pageable: Pageable): Page<CouponModel>
    fun findAllByIds(ids: List<Long>): List<CouponModel>
}
