package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Column
import jakarta.persistence.Embeddable

@Embeddable
data class Stock(
    @Column(name = "stock_quantity", nullable = false)
    val quantity: Int,
) {
    init {
        if (quantity < 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "재고는 0 이상이어야 합니다.")
        }
    }

    fun isEnough(amount: Int): Boolean = quantity >= amount
}
