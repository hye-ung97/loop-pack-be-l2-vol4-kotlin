package com.loopers.domain.product

import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable

interface ProductRepository {
    fun save(product: ProductModel): ProductModel
    fun findOnSaleById(id: Long): ProductModel?
    fun findActiveById(id: Long): ProductModel?
    fun findAllOnSaleByIds(ids: List<Long>): List<ProductModel>
    fun findActiveByIds(ids: List<Long>): List<ProductModel>
    fun findOnSaleProducts(brandId: Long?, pageable: Pageable): Page<ProductModel>
    fun findActiveProducts(brandId: Long?, pageable: Pageable): Page<ProductModel>
    fun findAllActiveByBrandId(brandId: Long): List<ProductModel>
}
