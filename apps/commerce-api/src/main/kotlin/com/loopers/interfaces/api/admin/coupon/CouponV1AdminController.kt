package com.loopers.interfaces.api.admin.coupon

import com.loopers.application.coupon.CouponAdminFacade
import com.loopers.interfaces.api.ApiResponse
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Sort
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController
import java.time.ZoneId

@RestController
@RequestMapping("/api-admin/v1/coupons")
class CouponV1AdminController(
    private val couponAdminFacade: CouponAdminFacade,
) : CouponV1AdminApiSpec {
    @PostMapping
    override fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: CouponV1AdminDto.CreateRequest,
    ): ApiResponse<CouponV1AdminDto.CouponResponse> {
        return couponAdminFacade.create(
            name = request.name,
            discountType = request.type,
            discountValue = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt.atZone(KST),
        ).let { CouponV1AdminDto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{couponId}")
    override fun getCoupon(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1AdminDto.CouponResponse> {
        return couponAdminFacade.getById(couponId)
            .let { CouponV1AdminDto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getCoupons(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.CouponPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = couponAdminFacade.getAll(pageable)
        val response = CouponV1AdminDto.CouponPageResponse(
            content = result.content.map(CouponV1AdminDto.CouponResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }

    @PutMapping("/{couponId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
        @RequestBody request: CouponV1AdminDto.UpdateRequest,
    ): ApiResponse<CouponV1AdminDto.CouponResponse> {
        return couponAdminFacade.update(
            id = couponId,
            name = request.name,
            discountType = request.type,
            discountValue = request.value,
            minOrderAmount = request.minOrderAmount,
            expiredAt = request.expiredAt.atZone(KST),
        ).let { CouponV1AdminDto.CouponResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{couponId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
    ): ApiResponse<Unit> {
        couponAdminFacade.delete(couponId)
        return ApiResponse.success(Unit)
    }

    @GetMapping("/{couponId}/issues")
    override fun getIssues(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.IssuePageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = couponAdminFacade.getIssues(couponId, pageable)
        val response = CouponV1AdminDto.IssuePageResponse(
            content = result.content.map(CouponV1AdminDto.IssueResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }

    companion object {
        private val KST: ZoneId = ZoneId.of("Asia/Seoul")
    }
}
