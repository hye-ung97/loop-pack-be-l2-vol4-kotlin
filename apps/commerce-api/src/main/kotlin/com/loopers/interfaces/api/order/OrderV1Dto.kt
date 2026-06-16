package com.loopers.interfaces.api.order

import com.loopers.application.order.OrderInfo
import com.loopers.domain.order.OrderStatus
import java.time.ZonedDateTime

class OrderV1Dto {
    data class PlaceOrderRequest(
        val items: List<OrderLineRequest>,
        val couponId: Long? = null,
    )

    data class OrderLineRequest(
        val productId: Long,
        val quantity: Int,
    )

    data class OrderItemResponse(
        val productId: Long,
        val productName: String,
        val unitPrice: Long,
        val quantity: Int,
        val lineTotal: Long,
    ) {
        companion object {
            fun from(item: OrderInfo.OrderItemInfo) = OrderItemResponse(
                productId = item.productId,
                productName = item.productName,
                unitPrice = item.unitPrice,
                quantity = item.quantity,
                lineTotal = item.lineTotal,
            )
        }
    }

    data class OrderResponse(
        val id: Long,
        val userId: Long,
        val status: OrderStatus,
        val totalPrice: Long,
        val discountAmount: Long,
        val finalAmount: Long,
        val couponId: Long?,
        val items: List<OrderItemResponse>,
        val createdAt: ZonedDateTime,
    ) {
        companion object {
            fun from(info: OrderInfo) = OrderResponse(
                id = info.id,
                userId = info.userId,
                status = info.status,
                totalPrice = info.totalPrice,
                discountAmount = info.discountAmount,
                finalAmount = info.finalAmount,
                couponId = info.couponId,
                items = info.items.map(OrderItemResponse::from),
                createdAt = info.createdAt,
            )
        }
    }

    data class OrderPageResponse(
        val content: List<OrderResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
