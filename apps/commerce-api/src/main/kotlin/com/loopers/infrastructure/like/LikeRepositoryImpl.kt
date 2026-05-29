package com.loopers.infrastructure.like

import com.loopers.domain.like.LikeModel
import com.loopers.domain.like.LikeRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class LikeRepositoryImpl(
    private val likeJpaRepository: LikeJpaRepository,
) : LikeRepository {
    override fun save(like: LikeModel): LikeModel = likeJpaRepository.save(like)

    override fun existsByUserIdAndProductId(userId: Long, productId: Long): Boolean =
        likeJpaRepository.existsByUserIdAndProductId(userId, productId)

    override fun findAllByUserId(userId: Long, pageable: Pageable): Page<LikeModel> =
        likeJpaRepository.findAllByUserIdOrderByIdDesc(userId, pageable)

    override fun deleteByUserIdAndProductId(userId: Long, productId: Long): Int =
        likeJpaRepository.deleteByUserIdAndProductId(userId, productId)
}
