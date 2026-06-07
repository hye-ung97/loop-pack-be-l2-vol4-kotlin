package com.loopers.application.product

import com.loopers.domain.brand.BrandModel
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
class ProductAdminFacade(
    private val productService: ProductService,
    private val brandService: BrandService,
) {
    @Transactional
    fun create(
        brandId: Long,
        name: String,
        price: Long,
        stockQuantity: Int,
        status: ProductStatus,
    ): ProductInfo {
        val brand = brandService.getById(brandId)
        val product = productService.register(brandId, name, price, stockQuantity, status)
        return ProductInfo.from(product, brand)
    }

    fun getById(productId: Long): ProductInfo {
        val product = productService.getActiveById(productId)
        val brand = brandService.getById(product.brandId)
        return ProductInfo.from(product, brand)
    }

    fun getProducts(brandId: Long?, pageable: Pageable): Page<ProductInfo> {
        val productsPage = productService.getActiveProducts(brandId, pageable)
        return mapToProductInfoPage(productsPage)
    }

    @Transactional
    fun update(productId: Long, name: String?, price: Long?, status: ProductStatus?): ProductInfo {
        productService.update(productId, name, price, status)
        return getById(productId)
    }

    @Transactional
    fun delete(productId: Long) {
        productService.delete(productId)
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
