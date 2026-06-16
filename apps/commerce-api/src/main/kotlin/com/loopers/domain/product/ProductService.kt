package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.data.domain.Page
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
@Transactional(readOnly = true)
class ProductService(
    private val productRepository: ProductRepository,
) {
    @Transactional
    fun register(
        brandId: Long,
        name: String,
        price: Long,
        stockQuantity: Int,
        status: ProductStatus = ProductStatus.ON_SALE,
    ): ProductModel {
        val product = ProductModel(
            brandId = brandId,
            name = name,
            price = price,
            stock = Stock(stockQuantity),
            status = status,
        )
        return productRepository.save(product)
    }

    fun getOnSaleById(id: Long): ProductModel {
        return productRepository.findOnSaleById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
    }

    fun getActiveById(id: Long): ProductModel {
        return productRepository.findActiveById(id)
            ?: throw CoreException(ErrorType.NOT_FOUND, "존재하지 않는 상품입니다.")
    }

    fun findAllOnSaleByIds(ids: List<Long>): List<ProductModel> = productRepository.findAllOnSaleByIds(ids)

    fun findActiveByIds(ids: List<Long>): List<ProductModel> = productRepository.findActiveByIds(ids)

    fun getOnSaleProducts(brandId: Long?, sort: ProductSort, pageable: Pageable): Page<ProductModel> {
        val sortedPageable = PageRequest.of(pageable.pageNumber, pageable.pageSize, sort.sort)
        return productRepository.findOnSaleProducts(brandId, sortedPageable)
    }

    fun getActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductModel> =
        productRepository.findActiveProducts(brandId, pageable)

    @Transactional
    fun update(id: Long, name: String?, price: Long?, status: ProductStatus?) {
        val product = getActiveById(id)
        name?.let { product.rename(it) }
        price?.let { product.changePrice(it) }
        status?.let { product.changeStatus(it) }
    }

    @Transactional
    fun delete(id: Long) {
        val product = productRepository.findActiveById(id) ?: return
        product.delete()
    }

    @Transactional
    fun deductStock(id: Long, quantity: Int) {
        if (productRepository.decreaseStockIfEnough(id, quantity) == 0) {
            throw CoreException(ErrorType.CONFLICT, "재고가 부족하거나 존재하지 않는 상품입니다.")
        }
    }

    @Transactional
    fun restoreStock(id: Long, quantity: Int) {
        productRepository.increaseStock(id, quantity)
    }

    @Transactional
    fun increaseLikeCount(id: Long) {
        productRepository.increaseLikeCount(id)
    }

    @Transactional
    fun decreaseLikeCount(id: Long) {
        productRepository.decreaseLikeCountIfPositive(id)
    }

    @Transactional
    fun softDeleteAllByBrandId(brandId: Long) {
        productRepository.findAllActiveByBrandId(brandId).forEach { it.delete() }
    }
}
