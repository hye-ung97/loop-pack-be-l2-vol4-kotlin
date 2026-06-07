package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSort
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component

@Component
class ProductFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    fun getProductDetail(productId: Long): ProductInfo {
        val product = productService.getOnSaleById(productId)
        val brand = brandService.getById(product.brandId)
        return ProductInfo.from(product, brand)
    }

    fun getProducts(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<ProductInfo> {
        val productsPage = productService.getOnSaleProducts(brandId, sort, pageable)
        return mapToProductInfoPage(productsPage)
    }

    private fun mapToProductInfoPage(productsPage: Page<ProductModel>): Page<ProductInfo> {
        val brandIds = productsPage.content.map { it.brandId }.distinct()
        val brandsById: Map<Long, BrandModel> = brandService.findAllActiveByIds(brandIds).associateBy { it.id }
        return productsPage.map { product ->
            val brand = brandsById[product.brandId]
                ?: throw CoreException(ErrorType.INTERNAL_ERROR, "상품(${product.id}) 의 브랜드(${product.brandId}) 가 존재하지 않습니다.")
            ProductInfo.from(product, brand)
        }
    }
}
