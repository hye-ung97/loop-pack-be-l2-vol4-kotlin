package com.loopers.infrastructure.brand

import com.loopers.domain.brand.BrandModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface BrandJpaRepository : JpaRepository<BrandModel, Long> {
    @Query("SELECT b FROM BrandModel b WHERE b.id = :id AND b.deletedAt IS NULL")
    fun findActiveById(id: Long): BrandModel?

    @Query(
        value = "SELECT b FROM BrandModel b WHERE b.deletedAt IS NULL",
        countQuery = "SELECT COUNT(b) FROM BrandModel b WHERE b.deletedAt IS NULL",
    )
    fun findAllActive(pageable: Pageable): Page<BrandModel>

    fun findByName(name: String): BrandModel?

    @Query("SELECT b FROM BrandModel b WHERE b.id IN :ids AND b.deletedAt IS NULL")
    fun findAllActiveByIds(ids: List<Long>): List<BrandModel>
}
