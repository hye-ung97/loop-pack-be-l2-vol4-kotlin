package com.loopers.domain.order

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import org.springframework.transaction.annotation.Transactional
import java.time.ZonedDateTime

@SpringBootTest
class OrderServiceIntegrationTest @Autowired constructor(
    private val orderService: OrderService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun item(productId: Long = 1L, name: String = "P1", unitPrice: Long = 1_000, quantity: Int = 2) =
        OrderItemModel(productId, name, unitPrice, quantity)

    @DisplayName("주문을 생성할 때,")
    @Nested
    inner class CreatePending {
        @DisplayName("유효한 항목으로 생성하면, PENDING 상태로 저장된다.")
        @Test
        fun savesAsPending() {
            // act
            val saved = orderService.createPending(1L, listOf(item(quantity = 2, unitPrice = 1_000)))

            // assert
            assertAll(
                { assertThat(saved.id).isNotNull() },
                { assertThat(saved.status).isEqualTo(OrderStatus.PENDING) },
                { assertThat(saved.totalPrice).isEqualTo(2_000L) },
            )
        }
    }

    @DisplayName("본인 주문을 ID로 조회할 때,")
    @Nested
    inner class GetByIdAndUserId {
        @DisplayName("본인 주문이 존재하면, 반환된다.")
        @Test
        @Transactional
        fun returnsOrder_whenOwned() {
            // arrange
            val saved = orderService.createPending(1L, listOf(item()))

            // act
            val result = orderService.getByIdAndUserId(saved.id, 1L)

            // assert
            assertAll(
                { assertThat(result.id).isEqualTo(saved.id) },
                { assertThat(result.items).hasSize(1) },
            )
        }

        @DisplayName("타인의 주문 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenNotOwned() {
            // arrange
            val saved = orderService.createPending(1L, listOf(item()))

            // act
            val result = assertThrows<CoreException> { orderService.getByIdAndUserId(saved.id, 999L) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않는 주문이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenAbsent() {
            val result = assertThrows<CoreException> { orderService.getByIdAndUserId(999L, 1L) }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("기간 조건으로 본인 주문 목록을 조회할 때,")
    @Nested
    inner class GetByUserIdAndPeriod {
        @DisplayName("기간 내 본인 주문만 반환된다.")
        @Test
        fun returnsWithinPeriod() {
            // arrange
            val mine = orderService.createPending(1L, listOf(item()))
            val others = orderService.createPending(2L, listOf(item()))

            val now = ZonedDateTime.now()
            val startAt = now.minusDays(1)
            val endAt = now.plusDays(1)

            // act
            val result = orderService.getByUserIdAndPeriod(1L, startAt, endAt, PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.first().id).isEqualTo(mine.id) },
                { assertThat(result.content.none { it.id == others.id }).isTrue() },
            )
        }
    }

    @DisplayName("어드민이 전체 주문을 조회할 때,")
    @Nested
    inner class GetAll {
        @DisplayName("모든 활성 주문이 반환된다.")
        @Test
        fun returnsAllActive() {
            // arrange
            orderService.createPending(1L, listOf(item()))
            orderService.createPending(2L, listOf(item()))

            // act
            val result = orderService.getAll(PageRequest.of(0, 10))

            // assert
            assertThat(result.totalElements).isEqualTo(2)
        }
    }
}
