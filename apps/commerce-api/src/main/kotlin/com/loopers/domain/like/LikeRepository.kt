package com.loopers.domain.like

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface LikeRepository {
    fun save(like: LikeModel): LikeModel
    fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean
    fun findAllByUserId(userId: Long, pageable: Pageable): Page<LikeModel>
    fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int
}
