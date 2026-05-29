package com.loopers.application.order

import com.loopers.domain.order.OrderService
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class OrderAdminFacade(
    private val orderService: OrderService,
) {
    @Transactional(readOnly = true)
    fun getById(orderId: Long): OrderInfo = OrderInfo.from(orderService.getById(orderId))

    @Transactional(readOnly = true)
    fun getAll(pageable: Pageable): Page<OrderInfo> = orderService.getAll(pageable).map(OrderInfo::from)
}
