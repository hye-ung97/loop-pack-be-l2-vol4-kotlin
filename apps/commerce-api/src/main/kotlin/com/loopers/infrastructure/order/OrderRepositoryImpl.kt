package com.loopers.infrastructure.order

import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderRepository
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.ZonedDateTime

@Repository
class OrderRepositoryImpl(
    private val orderJpaRepository: OrderJpaRepository,
) : OrderRepository {
    override fun save(order: OrderModel): OrderModel = orderJpaRepository.save(order)

    override fun findActiveByIdAndUserId(orderId: Long, userId: Long): OrderModel? =
        orderJpaRepository.findActiveByIdAndUserId(orderId, userId)

    override fun findActiveById(orderId: Long): OrderModel? = orderJpaRepository.findActiveById(orderId)

    override fun findActiveByUserIdAndPeriod(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel> = orderJpaRepository.findActiveByUserIdAndPeriod(userId, startAt, endAt, pageable)

    override fun findAllActive(pageable: Pageable): Page<OrderModel> = orderJpaRepository.findAllActive(pageable)
}
