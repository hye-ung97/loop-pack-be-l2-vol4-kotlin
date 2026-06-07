package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderAdminFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api-admin/v1/orders")
class OrderV1AdminController(
    private val orderAdminFacade: OrderAdminFacade,
) : OrderV1AdminApiSpec {
    @GetMapping("/{orderId}")
    override fun getOrder(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1AdminDto.OrderResponse> {
        return orderAdminFacade.getById(orderId)
            .let { OrderV1AdminDto.OrderResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getOrders(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<OrderV1AdminDto.OrderPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = orderAdminFacade.getAll(pageable)
        val response = OrderV1AdminDto.OrderPageResponse(
            content = result.content.map(OrderV1AdminDto.OrderResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }
}
