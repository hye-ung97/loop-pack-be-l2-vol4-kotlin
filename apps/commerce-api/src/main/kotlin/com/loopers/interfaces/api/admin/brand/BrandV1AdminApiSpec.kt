package com.loopers.interfaces.api.admin.brand

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RequestParam

@Tag(name = "Brand Admin V1", description = "브랜드 관리(어드민) API")
interface BrandV1AdminApiSpec {
    @Operation(summary = "브랜드 등록")
    fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: BrandV1AdminDto.CreateRequest,
    ): ApiResponse<BrandV1AdminDto.BrandResponse>

    @Operation(summary = "브랜드 단건 조회")
    fun getBrand(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1AdminDto.BrandResponse>

    @Operation(summary = "브랜드 목록 조회")
    fun getBrands(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<BrandV1AdminDto.BrandPageResponse>

    @Operation(summary = "브랜드 수정")
    fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
        @RequestBody request: BrandV1AdminDto.UpdateRequest,
    ): ApiResponse<BrandV1AdminDto.BrandResponse>

    @Operation(summary = "브랜드 삭제 (소속 상품도 함께 soft delete)")
    fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
    ): ApiResponse<Unit>
}
