package com.loopers.interfaces.api.product

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Product V1", description = "상품 API")
interface ProductV1ApiSpec {
    @Operation(summary = "상품 단건 조회")
    fun getProduct(@PathVariable productId: Long): ApiResponse<ProductV1Dto.ProductResponse>

    @Operation(summary = "상품 목록 조회 (필터·정렬·페이징)")
    fun getProducts(
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(required = false) sort: String?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductV1Dto.ProductPageResponse>
}
