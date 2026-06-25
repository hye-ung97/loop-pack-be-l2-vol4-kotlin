package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Index
import jakarta.persistence.Table

@Entity
@Table(
    name = "products",
    indexes = [
        Index(name = "idx_products_brand_like", columnList = "brand_id, like_count"),
        Index(name = "idx_products_brand_price", columnList = "brand_id, price"),
        Index(name = "idx_products_like_count", columnList = "like_count"),
    ],
)
class ProductModel(
    brandId: Long,
    name: String,
    price: Long,
    stock: Stock,
    status: ProductStatus = ProductStatus.ON_SALE,
) : BaseEntity() {
    @Column(name = "brand_id", nullable = false)
    var brandId: Long = brandId
        protected set

    var name: String = name
        protected set

    var price: Long = price
        protected set

    @Embedded
    var stock: Stock = stock
        protected set

    @Enumerated(EnumType.STRING)
    var status: ProductStatus = status
        protected set

    @Column(name = "like_count", nullable = false)
    var likeCount: Long = 0L
        protected set

    init {
        requireValidName(name)
        requireValidPrice(price)
    }

    fun rename(newName: String) {
        requireValidName(newName)
        this.name = newName
    }

    fun changePrice(newPrice: Long) {
        requireValidPrice(newPrice)
        this.price = newPrice
    }

    fun changeStatus(newStatus: ProductStatus) {
        this.status = newStatus
    }

    fun isOrderable(quantity: Int): Boolean =
        deletedAt == null && status == ProductStatus.ON_SALE && stock.isEnough(quantity)

    private fun requireValidName(name: String) {
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 이름은 비어있을 수 없습니다.")
        }
    }

    private fun requireValidPrice(price: Long) {
        if (price < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "가격은 0 이상이어야 합니다.")
        }
    }
}
