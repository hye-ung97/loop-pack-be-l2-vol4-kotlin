package com.loopers.domain.order

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.CascadeType
import jakarta.persistence.Column
import jakarta.persistence.ConstraintMode
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.ForeignKey
import jakarta.persistence.JoinColumn
import jakarta.persistence.OneToMany
import jakarta.persistence.Table

@Entity
@Table(name = "orders")
class OrderModel protected constructor(
    userId: Long,
    items: List<OrderItemModel>,
    couponId: Long?,
    discountAmount: Long,
) : BaseEntity() {
    @Column(name = "user_id", nullable = false)
    var userId: Long = userId
        protected set

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: OrderStatus = OrderStatus.PENDING
        protected set

    @Column(name = "total_price", nullable = false)
    var totalPrice: Long = items.sumOf { it.lineTotal }
        protected set

    @Column(name = "discount_amount", nullable = false)
    var discountAmount: Long = discountAmount
        protected set

    @Column(name = "final_amount", nullable = false)
    var finalAmount: Long = items.sumOf { it.lineTotal } - discountAmount
        protected set

    @Column(name = "coupon_id")
    var couponId: Long? = couponId
        protected set

    @OneToMany(cascade = [CascadeType.ALL], orphanRemoval = true)
    @JoinColumn(name = "order_id", nullable = false, foreignKey = ForeignKey(ConstraintMode.NO_CONSTRAINT))
    private val mutableItems: MutableList<OrderItemModel> = items.toMutableList()

    val items: List<OrderItemModel> get() = mutableItems.toList()

    init {
        if (items.isEmpty()) {
            throw CoreException(ErrorType.BAD_REQUEST, "주문 항목은 최소 1개 이상이어야 합니다.")
        }
        if (discountAmount < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 0 이상이어야 합니다.")
        }
        if (discountAmount > totalPrice) {
            throw CoreException(ErrorType.BAD_REQUEST, "할인 금액은 주문 금액을 초과할 수 없습니다.")
        }
    }

    fun pay() {
        if (status != OrderStatus.PENDING) {
            throw CoreException(ErrorType.CONFLICT, "PENDING 상태만 결제할 수 있습니다.")
        }
        status = OrderStatus.PAID
    }

    fun cancel() {
        if (status == OrderStatus.CANCELED) return
        status = OrderStatus.CANCELED
    }

    companion object {
        fun create(
            userId: Long,
            items: List<OrderItemModel>,
            couponId: Long? = null,
            discountAmount: Long = 0,
        ): OrderModel = OrderModel(userId, items, couponId, discountAmount)
    }
}
