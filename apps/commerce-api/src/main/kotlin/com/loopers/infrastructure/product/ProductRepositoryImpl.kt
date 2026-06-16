package com.loopers.infrastructure.product

import com.loopers.domain.product.ProductModel
import com.loopers.domain.product.ProductRepository
import com.loopers.domain.product.ProductStatus
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ProductRepositoryImpl(
    private val productJpaRepository: ProductJpaRepository,
) : ProductRepository {
    override fun save(product: ProductModel): ProductModel = productJpaRepository.save(product)

    override fun findOnSaleById(id: Long): ProductModel? =
        productJpaRepository.findActiveByIdAndStatus(id, ProductStatus.ON_SALE)

    override fun findActiveById(id: Long): ProductModel? = productJpaRepository.findActiveById(id)

    override fun findAllOnSaleByIds(ids: List<Long>): List<ProductModel> =
        if (ids.isEmpty()) emptyList() else productJpaRepository.findAllActiveByIdsAndStatus(ids, ProductStatus.ON_SALE)

    override fun findActiveByIds(ids: List<Long>): List<ProductModel> =
        if (ids.isEmpty()) emptyList() else productJpaRepository.findAllActiveByIds(ids)

    override fun findOnSaleProducts(brandId: Long?, pageable: Pageable): Page<ProductModel> =
        productJpaRepository.findActiveProductsByStatus(brandId, ProductStatus.ON_SALE, pageable)

    override fun findActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductModel> =
        productJpaRepository.findActiveProducts(brandId, pageable)

    override fun findAllActiveByBrandId(brandId: Long): List<ProductModel> =
        productJpaRepository.findAllActiveByBrandId(brandId)

    override fun decreaseStockIfEnough(id: Long, quantity: Int): Int =
        productJpaRepository.decreaseStockIfEnough(id, quantity)

    override fun increaseStock(id: Long, quantity: Int): Int = productJpaRepository.increaseStock(id, quantity)

    override fun increaseLikeCount(id: Long): Int = productJpaRepository.increaseLikeCount(id)

    override fun decreaseLikeCountIfPositive(id: Long): Int = productJpaRepository.decreaseLikeCountIfPositive(id)
}
