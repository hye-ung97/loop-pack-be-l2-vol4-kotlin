package com.loopers.interfaces.api.like

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Like V1", description = "좋아요 API")
interface LikeV1ApiSpec {
    @Operation(summary = "상품 좋아요 등록 (멱등)")
    fun like(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit>

    @Operation(summary = "상품 좋아요 취소 (멱등)")
    fun unlike(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit>

    @Operation(summary = "본인이 좋아요 한 상품 목록 조회")
    fun getMyLikedProducts(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<LikeV1Dto.LikedProductPageResponse>
}
