package com.loopers.interfaces.api.admin.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Product Admin V1", description = "상품 관리(어드민) API")
interface ProductV1AdminApiSpec {
    @Operation(summary = "상품 등록")
    fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: ProductV1AdminDto.CreateRequest,
    ): ApiResponse<ProductV1AdminDto.ProductResponse>

    @Operation(summary = "상품 단건 조회")
    fun getProduct(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1AdminDto.ProductResponse>

    @Operation(summary = "상품 목록 조회")
    fun getProducts(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductV1AdminDto.ProductPageResponse>

    @Operation(summary = "상품 수정 (brandId 는 변경 불가)")
    fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
        @RequestBody request: ProductV1AdminDto.UpdateRequest,
    ): ApiResponse<ProductV1AdminDto.ProductResponse>

    @Operation(summary = "상품 삭제")
    fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit>
}
