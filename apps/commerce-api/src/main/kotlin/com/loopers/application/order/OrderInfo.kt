package com.loopers.application.order

import com.loopers.domain.order.OrderItemModel
import com.loopers.domain.order.OrderModel
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

data class OrderInfo(
    val id: Long,
    val userId: Long,
    val status: OrderStatus,
    val totalPrice: Long,
    val items: List<OrderItemInfo>,
    val createdAt: ZonedDateTime,
) {
    data class OrderItemInfo(
        val productId: Long,
        val productName: String,
        val unitPrice: Long,
        val quantity: Int,
        val lineTotal: Long,
    ) {
        companion object {
            fun from(item: OrderItemModel): OrderItemInfo = OrderItemInfo(
                productId = item.productId,
                productName = item.productNameSnapshot,
                unitPrice = item.unitPriceSnapshot,
                quantity = item.quantity,
                lineTotal = item.lineTotal,
            )
        }
    }

    companion object {
        fun from(order: OrderModel): OrderInfo = OrderInfo(
            id = order.id,
            userId = order.userId,
            status = order.status,
            totalPrice = order.totalPrice,
            items = order.items.map(OrderItemInfo::from),
            createdAt = order.createdAt,
        )
    }
}
