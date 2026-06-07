package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.ZonedDateTime

interface OrderJpaRepository : JpaRepository<OrderModel, Long> {
    @Query("SELECT o FROM OrderModel o WHERE o.id = :orderId AND o.userId = :userId AND o.deletedAt IS NULL")
    fun findActiveByIdAndUserId(
        @Param("orderId") orderId: Long,
        @Param("userId") userId: Long,
    ): OrderModel?

    @Query("SELECT o FROM OrderModel o WHERE o.id = :orderId AND o.deletedAt IS NULL")
    fun findActiveById(@Param("orderId") orderId: Long): OrderModel?

    @Query(
        value = """
            SELECT o FROM OrderModel o
            WHERE o.userId = :userId
              AND o.deletedAt IS NULL
              AND o.createdAt >= :startAt
              AND o.createdAt < :endAt
        """,
        countQuery = """
            SELECT COUNT(o) FROM OrderModel o
            WHERE o.userId = :userId
              AND o.deletedAt IS NULL
              AND o.createdAt >= :startAt
              AND o.createdAt < :endAt
        """,
    )
    fun findActiveByUserIdAndPeriod(
        @Param("userId") userId: Long,
        @Param("startAt") startAt: ZonedDateTime,
        @Param("endAt") endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel>

    @Query(
        value = "SELECT o FROM OrderModel o WHERE o.deletedAt IS NULL",
        countQuery = "SELECT COUNT(o) FROM OrderModel o WHERE o.deletedAt IS NULL",
    )
    fun findAllActive(pageable: Pageable): Page<OrderModel>
}
