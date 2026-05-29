package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@Component
@Transactional(readOnly = true)
class OrderService(
    private val orderRepository: OrderRepository,
) {
    @Transactional
    fun createPending(userId: Long, items: List<OrderItemModel>): OrderModel {
        val order = OrderModel.create(userId, items)
        return orderRepository.save(order)
    }

    fun getByIdAndUserId(orderId: Long, userId: Long): OrderModel {
        return orderRepository.findActiveByIdAndUserId(orderId, userId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    fun getById(orderId: Long): OrderModel {
        return orderRepository.findActiveById(orderId)
            ?: throw CoreException(ErrorType.NOT_FOUND, "주문을 찾을 수 없습니다.")
    }

    fun getByUserIdAndPeriod(
        userId: Long,
        startAt: ZonedDateTime,
        endAt: ZonedDateTime,
        pageable: Pageable,
    ): Page<OrderModel> = orderRepository.findActiveByUserIdAndPeriod(userId, startAt, endAt, pageable)

    fun getAll(pageable: Pageable): Page<OrderModel> = orderRepository.findAllActive(pageable)
}
