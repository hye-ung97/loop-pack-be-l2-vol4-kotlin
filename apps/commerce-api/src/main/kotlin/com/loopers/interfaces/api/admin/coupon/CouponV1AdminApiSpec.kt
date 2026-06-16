package com.loopers.interfaces.api.admin.coupon

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Coupon Admin V1", description = "쿠폰 관리(어드민) API")
interface CouponV1AdminApiSpec {
    @Operation(summary = "쿠폰 템플릿 등록")
    fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: CouponV1AdminDto.CreateRequest,
    ): ApiResponse<CouponV1AdminDto.CouponResponse>

    @Operation(summary = "쿠폰 템플릿 단건 조회")
    fun getCoupon(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
    ): ApiResponse<CouponV1AdminDto.CouponResponse>

    @Operation(summary = "쿠폰 템플릿 목록 조회")
    fun getCoupons(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.CouponPageResponse>

    @Operation(summary = "쿠폰 템플릿 수정")
    fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
        @RequestBody request: CouponV1AdminDto.UpdateRequest,
    ): ApiResponse<CouponV1AdminDto.CouponResponse>

    @Operation(summary = "쿠폰 템플릿 삭제")
    fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
    ): ApiResponse<Unit>

    @Operation(summary = "특정 쿠폰의 발급 내역 조회")
    fun getIssues(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable couponId: Long,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<CouponV1AdminDto.IssuePageResponse>
}
