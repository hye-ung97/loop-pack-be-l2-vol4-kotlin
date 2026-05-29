package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface LikeJpaRepository : JpaRepository<LikeModel, Long> {
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean

    fun findAllByUserIdOrderByIdDesc(userId: Long, pageable: Pageable): Page<LikeModel>

    @Modifying
    @Query("DELETE FROM LikeModel l WHERE l.userId = :userId AND l.productId = :productId")
    fun deleteByUserIdAndProductId(
        @Param("userId") userId: Long,
        @Param("productId") productId: Long,
    ): Int
}
