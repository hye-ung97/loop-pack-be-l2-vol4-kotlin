package com.loopers.interfaces.api.admin.order

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Order Admin V1", description = "주문 관리(어드민) API")
interface OrderV1AdminApiSpec {
    @Operation(summary = "주문 단건 조회")
    fun getOrder(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable orderId: Long,
    ): ApiResponse<OrderV1AdminDto.OrderResponse>

    @Operation(summary = "전체 주문 목록 조회")
    fun getOrders(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<OrderV1AdminDto.OrderPageResponse>
}
