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

    fun deduct(amount: Int): Stock {
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "차감 수량은 1 이상이어야 합니다.")
        }
        if (quantity < amount) {
            throw CoreException(ErrorType.CONFLICT, "재고가 부족합니다.")
        }
        return Stock(quantity - amount)
    }

    fun restore(amount: Int): Stock {
        if (amount <= 0) {
            throw CoreException(ErrorType.BAD_REQUEST, "복구 수량은 1 이상이어야 합니다.")
        }
        return Stock(quantity + amount)
    }

    fun isEnough(amount: Int): Boolean = quantity >= amount
}
