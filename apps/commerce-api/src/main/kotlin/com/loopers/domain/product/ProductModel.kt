package com.loopers.domain.product

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embedded
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.Table

@Entity
@Table(name = "products")
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

    fun deduct(quantity: Int) {
        this.stock = stock.deduct(quantity)
    }

    fun restoreStock(quantity: Int) {
        this.stock = stock.restore(quantity)
    }

    fun increaseLikeCount() {
        this.likeCount += 1
    }

    fun decreaseLikeCount() {
        if (likeCount == 0L) {
            throw CoreException(ErrorType.BAD_REQUEST, "좋아요 수가 0 이하로 내려갈 수 없습니다.")
        }
        this.likeCount -= 1
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
