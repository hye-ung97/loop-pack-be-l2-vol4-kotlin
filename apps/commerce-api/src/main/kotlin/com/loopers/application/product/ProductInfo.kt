package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandModel
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductStatus

data class ProductInfo(
    val id: Long,
    val name: String,
    val price: Long,
    val stockQuantity: Int,
    val status: ProductStatus,
    val likeCount: Long,
    val brand: BrandInfo,
) {
    companion object {
        fun from(product: ProductModel, brand: BrandModel): ProductInfo = ProductInfo(
            id = product.id,
            name = product.name,
            price = product.price,
            stockQuantity = product.stock.quantity,
            status = product.status,
            likeCount = product.likeCount,
            brand = BrandInfo.from(brand),
        )
    }
}
