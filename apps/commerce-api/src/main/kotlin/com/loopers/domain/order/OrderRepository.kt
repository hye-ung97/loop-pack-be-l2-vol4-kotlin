package com.loopers.domain.order

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import java.time.ZonedDateTime

interface OrderRepository {
    fun save(order: OrderModel): OrderModel
    fun findActiveByIdAndUserId(orderId: Long, userId: Long): OrderModel?
    fun findActiveById(orderId: Long): OrderModel?
    fun findActiveByUserIdAndPeriod(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel>
    fun findAllActive(pageable: Pageable): Page<OrderModel>
}
