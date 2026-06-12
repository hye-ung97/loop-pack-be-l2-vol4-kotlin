package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType

enum class DiscountType {
    /** 정액: 할인 금액(원) */
    FIXED {
        override fun validateValue(value: Long) {
            if (value <= 0) {
                throw CoreException(ErrorType.BAD_REQUEST, "정액 할인 금액은 1 이상이어야 합니다.")
            }
        }

        override fun calculate(value: Long, orderAmount: Long): Long = minOf(value, orderAmount)
    },

    /** 정률: 할인 비율(%) */
    RATE {
        override fun validateValue(value: Long) {
            if (value <= 0 || value > 100) {
                throw CoreException(ErrorType.BAD_REQUEST, "정률 할인율은 1~100 사이여야 합니다.")
            }
        }

        override fun calculate(value: Long, orderAmount: Long): Long = orderAmount * value / 100
    }, ;

    abstract fun validateValue(value: Long)

    abstract fun calculate(value: Long, orderAmount: Long): Long
}
