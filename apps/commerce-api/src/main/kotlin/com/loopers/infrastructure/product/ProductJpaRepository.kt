package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ProductJpaRepository : JpaRepository<ProductModel, Long> {
    @Modifying
    @Query(
        """
            UPDATE ProductModel p
            SET p.stock.quantity = p.stock.quantity - :quantity
            WHERE p.id = :id AND p.deletedAt IS NULL AND p.stock.quantity >= :quantity
        """,
    )
    fun decreaseStockIfEnough(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

    @Modifying
    @Query("UPDATE ProductModel p SET p.stock.quantity = p.stock.quantity + :quantity WHERE p.id = :id AND p.deletedAt IS NULL")
    fun increaseStock(@Param("id") id: Long, @Param("quantity") quantity: Int): Int

    @Modifying
    @Query("UPDATE ProductModel p SET p.likeCount = p.likeCount + 1 WHERE p.id = :id AND p.deletedAt IS NULL")
    fun increaseLikeCount(@Param("id") id: Long): Int

    @Modifying
    @Query("UPDATE ProductModel p SET p.likeCount = p.likeCount - 1 WHERE p.id = :id AND p.deletedAt IS NULL AND p.likeCount > 0")
    fun decreaseLikeCountIfPositive(@Param("id") id: Long): Int

    @Query(
        "SELECT p FROM ProductModel p WHERE p.id = :id AND p.deletedAt IS NULL AND p.status = :status",
    )
    fun findActiveByIdAndStatus(
        @Param("id") id: Long,
        @Param("status") status: ProductStatus,
    ): ProductModel?

    @Query("SELECT p FROM ProductModel p WHERE p.id = :id AND p.deletedAt IS NULL")
    fun findActiveById(@Param("id") id: Long): ProductModel?

    @Query(
        "SELECT p FROM ProductModel p WHERE p.id IN :ids AND p.deletedAt IS NULL AND p.status = :status",
    )
    fun findAllActiveByIdsAndStatus(
        @Param("ids") ids: List<Long>,
        @Param("status") status: ProductStatus,
    ): List<ProductModel>

    @Query("SELECT p FROM ProductModel p WHERE p.id IN :ids AND p.deletedAt IS NULL")
    fun findAllActiveByIds(@Param("ids") ids: List<Long>): List<ProductModel>

    @Query(
        value = """
            SELECT p FROM ProductModel p
            WHERE p.deletedAt IS NULL
              AND p.status = :status
              AND (:brandId IS NULL OR p.brandId = :brandId)
        """,
        countQuery = """
            SELECT COUNT(p) FROM ProductModel p
            WHERE p.deletedAt IS NULL
              AND p.status = :status
              AND (:brandId IS NULL OR p.brandId = :brandId)
        """,
    )
    fun findActiveProductsByStatus(
        @Param("brandId") brandId: Long?,
        @Param("status") status: ProductStatus,
        pageable: Pageable,
    ): Page<ProductModel>

    @Query(
        value = """
            SELECT p FROM ProductModel p
            WHERE p.deletedAt IS NULL
              AND (:brandId IS NULL OR p.brandId = :brandId)
        """,
        countQuery = """
            SELECT COUNT(p) FROM ProductModel p
            WHERE p.deletedAt IS NULL
              AND (:brandId IS NULL OR p.brandId = :brandId)
        """,
    )
    fun findActiveProducts(
        @Param("brandId") brandId: Long?,
        pageable: Pageable,
    ): Page<ProductModel>

    @Query("SELECT p FROM ProductModel p WHERE p.brandId = :brandId AND p.deletedAt IS NULL")
    fun findAllActiveByBrandId(@Param("brandId") brandId: Long): List<ProductModel>
}
