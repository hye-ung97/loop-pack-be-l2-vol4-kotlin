package com.loopers.interfaces.api.coupon

import com.loopers.application.coupon.CouponFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
class CouponV1Controller(
    private val couponFacade: CouponFacade,
) : CouponV1ApiSpec {
    @PostMapping("/api/v1/coupons/{couponId}/issue")
    override fun issue(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1Dto.MyCouponResponse> {
        return couponFacade.issue(loginId, loginPw, couponId)
            .let { CouponV1Dto.MyCouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/api/v1/users/me/coupons")
    override fun getMyCoupons(
        @RequestHeader("X-Loopers-LoginId") loginId: String,
        @RequestHeader("X-Loopers-LoginPw") loginPw: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1Dto.MyCouponPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = couponFacade.getMyCoupons(loginId, loginPw, pageable)
        val response = CouponV1Dto.MyCouponPageResponse(
            content = result.content.map(CouponV1Dto.MyCouponResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }
}
