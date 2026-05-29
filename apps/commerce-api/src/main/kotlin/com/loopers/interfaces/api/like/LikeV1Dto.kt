package com.loopers.interfaces.api.like

import com.loopers.application.product.ProductInfo
import com.loopers.domain.product.ProductStatus

class LikeV1Dto {
    data class BrandSummary(
        val id: Long,
        val name: String,
    )

    data class LikedProductResponse(
        val id: Long,
        val name: String,
        val price: Long,
        val stockQuantity: Int,
        val status: ProductStatus,
        val likeCount: Long,
        val brand: BrandSummary,
    ) {
        companion object {
            fun from(info: ProductInfo): LikedProductResponse = LikedProductResponse(
                id = info.id,
                name = info.name,
                price = info.price,
                stockQuantity = info.stockQuantity,
                status = info.status,
                likeCount = info.likeCount,
                brand = BrandSummary(id = info.brand.id, name = info.brand.name),
            )
        }
    }

    data class LikedProductPageResponse(
        val content: List<LikedProductResponse>,
        val totalElements: Long,
        val totalPages: Int,
        val page: Int,
        val size: Int,
    )
}
