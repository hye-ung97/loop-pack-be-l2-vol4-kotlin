package com.loopers.application.order

import com.loopers.application.user.UserFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class OrderConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val userFacade: UserFacade,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val loginId = "user123"
    private val rawPassword = "Valid1!pw"

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun signUp() {
        userFacade.signUp(loginId, rawPassword, "홍길동", java.time.LocalDate.of(1994, 7, 14), "hong@example.com")
    }

    @DisplayName("동일 상품에 대해 여러 주문이 동시에 요청되어도, 재고가 정확히 차감되고 음수가 되지 않는다.")
    @Test
    fun stockIsDeductedCorrectly_underConcurrency() {
        // arrange
        signUp()
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 1_000, 5, ProductStatus.ON_SALE) // 재고 5
        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val success = AtomicInteger(0)
        val failure = AtomicInteger(0)

        // act: 10명이 동시에 1개씩 주문
        repeat(threadCount) {
            executor.submit {
                try {
                    orderFacade.place(
                        loginId,
                        rawPassword,
                        listOf(OrderFacade.PlaceOrderLine(productId = product.id, quantity = 1)),
                    )
                    success.incrementAndGet()
                } catch (e: Exception) {
                    failure.incrementAndGet()
                } finally {
                    latch.countDown()
                }
            }
        }
        latch.await()
        executor.shutdown()

        // assert: 재고 5개 → 정확히 5건 성공, 5건 실패, 최종 재고 0 (음수 없음)
        val reloaded = productJpaRepository.findById(product.id).orElseThrow()
        assertAll(
            { assertThat(success.get()).isEqualTo(5) },
            { assertThat(failure.get()).isEqualTo(5) },
            { assertThat(reloaded.stock.quantity).isEqualTo(0) },
        )
    }
}
