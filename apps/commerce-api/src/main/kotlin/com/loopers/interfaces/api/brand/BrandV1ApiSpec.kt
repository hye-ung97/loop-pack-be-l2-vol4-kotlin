package com.loopers.interfaces.api.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable

@Tag(name = "Brand V1", description = "브랜드 API")
interface BrandV1ApiSpec {
    @Operation(summary = "브랜드 단건 조회")
    fun getBrand(@PathVariable brandId: Long): ApiResponse<BrandV1Dto.BrandResponse>
}
