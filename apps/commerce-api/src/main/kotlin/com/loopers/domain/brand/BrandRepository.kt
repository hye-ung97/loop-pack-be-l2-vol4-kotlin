package com.loopers.domain.brand

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface BrandRepository {
    fun save(brand: BrandModel): BrandModel
    fun findActiveById(id: Long): BrandModel?
    fun findAllActive(pageable: Pageable): Page<BrandModel>
    fun findAllActiveByIds(ids: List<Long>): List<BrandModel>
    fun findByNameIncludingDeleted(name: String): BrandModel?
}
