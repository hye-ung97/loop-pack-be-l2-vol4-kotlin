package com.loopers.interfaces.api.admin.product

import com.loopers.application.product.ProductAdminFacade
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
@RequestMapping("/api-admin/v1/products")
class ProductV1AdminController(
    private val productAdminFacade: ProductAdminFacade,
) : ProductV1AdminApiSpec {
    @PostMapping
    override fun create(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestBody request: ProductV1AdminDto.CreateRequest,
    ): ApiResponse<ProductV1AdminDto.ProductResponse> {
        return productAdminFacade.create(
            brandId = request.brandId,
            name = request.name,
            price = request.price,
            stockQuantity = request.stockQuantity,
            status = request.status,
        )
            .let { ProductV1AdminDto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping("/{productId}")
    override fun getProduct(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
    ): ApiResponse<ProductV1AdminDto.ProductResponse> {
        return productAdminFacade.getById(productId)
            .let { ProductV1AdminDto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @GetMapping
    override fun getProducts(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @RequestParam(required = false) brandId: Long?,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): ApiResponse<ProductV1AdminDto.ProductPageResponse> {
        val pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"))
        val result = productAdminFacade.getProducts(brandId, pageable)
        val response = ProductV1AdminDto.ProductPageResponse(
            content = result.content.map(ProductV1AdminDto.ProductResponse::from),
            totalElements = result.totalElements,
            totalPages = result.totalPages,
            page = result.number,
            size = result.size,
        )
        return ApiResponse.success(response)
    }

    @PutMapping("/{productId}")
    override fun update(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
        @RequestBody request: ProductV1AdminDto.UpdateRequest,
    ): ApiResponse<ProductV1AdminDto.ProductResponse> {
        return productAdminFacade.update(productId, request.name, request.price, request.status)
            .let { ProductV1AdminDto.ProductResponse.from(it) }
            .let { ApiResponse.success(it) }
    }

    @DeleteMapping("/{productId}")
    override fun delete(
        @RequestHeader("X-Loopers-Ldap") ldapId: String,
        @PathVariable productId: Long,
    ): ApiResponse<Unit> {
        productAdminFacade.delete(productId)
        return ApiResponse.success(Unit)
    }
}
