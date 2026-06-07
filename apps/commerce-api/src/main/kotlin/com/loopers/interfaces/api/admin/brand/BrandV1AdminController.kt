package com.loopers.interfaces.api.admin.brand

import com.loopers.application.brand.BrandAdminFacade
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

@RestController
@RequestMapping("/api-admin/v1/brands")
class BrandV1AdminController(
    private val brandAdminFacade: BrandAdminFacade,
) : BrandV1AdminApiSpec {
    @PostMapping
    override fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: BrandV1AdminDto.CreateRequest,
    ): ApiResponse<BrandV1AdminDto.BrandResponse> {
        return brandAdminFacade.create(request.name)
            .let { BrandV1AdminDto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{brandId}")
    override fun getBrand(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
    ): ApiResponse<BrandV1AdminDto.BrandResponse> {
        return brandAdminFacade.getById(brandId)
            .let { BrandV1AdminDto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getBrands(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<BrandV1AdminDto.BrandPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"))
        val result = brandAdminFacade.getAll(pageable)
        val response = BrandV1AdminDto.BrandPageResponse(
            content = result.content.map(BrandV1AdminDto.BrandResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }

    @PutMapping("/{brandId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
        @RequestBody request: BrandV1AdminDto.UpdateRequest,
    ): ApiResponse<BrandV1AdminDto.BrandResponse> {
        brandAdminFacade.update(brandId, request.name)
        return brandAdminFacade.getById(brandId)
            .let { BrandV1AdminDto.BrandResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{brandId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable brandId: Long,
    ): ApiResponse<Unit> {
        brandAdminFacade.delete(brandId)
        return ApiResponse.success(Unit)
    }
}
