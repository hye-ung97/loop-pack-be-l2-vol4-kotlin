package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class OrderModelTest {
    private fun item(productId: Long = 1L, name: String = "P1", unitPrice: Long = 1_000, quantity: Int = 2): OrderItemModel =
        OrderItemModel(
            productId = productId,
            productNameSnapshot = name,
            unitPriceSnapshot = unitPrice,
            quantity = quantity,
        )

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 항목으로 생성하면, totalPrice가 라인 합계로 계산된다.")
        @Test
        fun calculatesTotalPrice() {
            // arrange
            val items = listOf(
                item(productId = 1L, unitPrice = 1_000, quantity = 2),
                item(productId = 2L, unitPrice = 500, quantity = 3),
            )

            // act
            val order = OrderModel.create(userId = 1L, items = items)

            // assert
            assertAll(
                { assertThat(order.status).isEqualTo(OrderStatus.PENDING) },
                { assertThat(order.totalPrice).isEqualTo(2 * 1_000L + 3 * 500L) },
                { assertThat(order.items).hasSize(2) },
            )
        }

        @DisplayName("항목이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenItemsAreEmpty() {
            // act
            val result = assertThrows<CoreException> { OrderModel.create(userId = 1L, items = emptyList()) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문 항목을 생성할 때,")
    @Nested
    inner class CreateItem {
        @DisplayName("lineTotal은 unitPriceSnapshot × quantity로 계산된다.")
        @Test
        fun calculatesLineTotal() {
            // act
            val orderItem = item(unitPrice = 1_500, quantity = 4)

            // assert
            assertThat(orderItem.lineTotal).isEqualTo(6_000L)
        }

        @DisplayName("스냅샷 이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            val result = assertThrows<CoreException> { item(name = "  ") }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("스냅샷 단가가 음수면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenUnitPriceIsNegative() {
            val result = assertThrows<CoreException> { item(unitPrice = -1) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("수량이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenQuantityIsZeroOrLess() {
            val result = assertThrows<CoreException> { item(quantity = 0) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문 상태를 전이할 때,")
    @Nested
    inner class Transition {
        @DisplayName("pay: PENDING -> PAID로 전이된다.")
        @Test
        fun pay_pendingToPaid() {
            val order = OrderModel.create(1L, listOf(item()))
            order.pay()
            assertThat(order.status).isEqualTo(OrderStatus.PAID)
        }

        @DisplayName("pay: PAID 상태에서 다시 결제하면, CONFLICT 예외가 발생한다.")
        @Test
        fun pay_throwsConflict_whenAlreadyPaid() {
            val order = OrderModel.create(1L, listOf(item()))
            order.pay()
            val result = assertThrows<CoreException> { order.pay() }
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("cancel: 어떤 상태에서든 CANCELED로 전이된다.")
        @Test
        fun cancel_anyToCanceled() {
            val order = OrderModel.create(1L, listOf(item()))
            order.cancel()
            assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
        }

        @DisplayName("cancel: 이미 취소된 상태에서 다시 취소해도, 예외 없이 처리된다 (멱등).")
        @Test
        fun cancel_isIdempotent() {
            val order = OrderModel.create(1L, listOf(item()))
            order.cancel()
            order.cancel()
            assertThat(order.status).isEqualTo(OrderStatus.CANCELED)
        }
    }
}
