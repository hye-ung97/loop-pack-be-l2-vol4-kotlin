package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table

@Entity
@Table(name = "order_items")
class OrderItemModel(
    productId: Long,
    productNameSnapshot: String,
    unitPriceSnapshot: Long,
    quantity: Int,
) : BaseEntity() {
    @Column(name = "product_id", nullable = false)
    var productId: Long = productId
        protected set

    @Column(name = "product_name_snapshot", nullable = false)
    var productNameSnapshot: String = productNameSnapshot
        protected set

    @Column(name = "unit_price_snapshot", nullable = false)
    var unitPriceSnapshot: Long = unitPriceSnapshot
        protected set

    @Column(name = "quantity", nullable = false)
    var quantity: Int = quantity
        protected set

    @Column(name = "line_total", nullable = false)
    var lineTotal: Long = unitPriceSnapshot * quantity
        protected set

    init {
        if (productNameSnapshot.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "상품 스냅샷 이름은 비어있을 수 없습니다.")
        }
        if (unitPriceSnapshot < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "스냅샷 단가는 0 이상이어야 합니다.")
        }
        if (quantity <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 수량은 1 이상이어야 합니다.")
        }
    }
}
