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

    /** 재고가 충분할 때만 원자적으로 차감한다. 반환값은 영향받은 행 수(1: 성공, 0: 재고 부족/없음). */
    fun decreaseStockIfEnough(id: Long, quantity: Int): Int

    /** 재고를 원자적으로 복구한다. */
    fun increaseStock(id: Long, quantity: Int): Int

    /** 좋아요 수를 원자적으로 1 증가시킨다. */
    fun increaseLikeCount(id: Long): Int

    /** 좋아요 수가 양수일 때만 원자적으로 1 감소시킨다. 반환값은 영향받은 행 수. */
    fun decreaseLikeCountIfPositive(id: Long): Int
}
