package com.loopers.interfaces.api.like

import com.loopers.application.like.LikeFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class LikeV1Controller(
    private val likeFacade: LikeFacade,
) : LikeV1ApiSpec {
    @PostMapping("/api/v1/products/{productId}/likes")
    override fun like(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.like(loginId, loginPw, productId)
        return ApiResponse.success(Unit)
    }

    @DeleteMapping("/api/v1/products/{productId}/likes")
    override fun unlike(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        likeFacade.unlike(loginId, loginPw, productId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/api/v1/users/{userId}/likes")
    override fun getMyLikedProducts(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable userId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<LikeV1Dto.LikedProductPageResponse> {
        val pageable = PageRequest.of(page, size)
        val result = likeFacade.getMyLikedProducts(loginId, loginPw, userId, pageable)
        val response = LikeV1Dto.LikedProductPageResponse(
            content = result.content.map(LikeV1Dto.LikedProductResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }
}
