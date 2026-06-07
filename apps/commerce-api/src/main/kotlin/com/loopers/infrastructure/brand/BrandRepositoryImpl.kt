package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class BrandRepositoryImpl(
    private val brandJpaRepository: BrandJpaRepository,
) : BrandRepository {
    override fun save(brand: BrandModel): BrandModel = brandJpaRepository.save(brand)

    override fun findActiveById(id: Long): BrandModel? = brandJpaRepository.findActiveById(id)

    override fun findAllActive(pageable: Pageable): Page<BrandModel> = brandJpaRepository.findAllActive(pageable)

    override fun findByNameIncludingDeleted(name: String): BrandModel? = brandJpaRepository.findByName(name)

    override fun findAllActiveByIds(ids: List<Long>): List<BrandModel> =
        if (ids.isEmpty()) emptyList() else brandJpaRepository.findAllActiveByIds(ids)
}
