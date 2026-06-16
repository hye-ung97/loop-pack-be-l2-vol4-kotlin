package com.loopers.application.coupon

import com.loopers.application.order.OrderFacade
import com.loopers.application.user.UserFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.domain.user.UserService
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest
import java.time.LocalDate
import java.time.ZonedDateTime
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

@SpringBootTest
class CouponConcurrencyTest @Autowired constructor(
    private val orderFacade: OrderFacade,
    private val userFacade: UserFacade,
    private val userService: UserService,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val couponService: CouponService,
    private val userCouponService: UserCouponService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val loginId = "user123"
    private val rawPassword = "Valid1!pw"

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("동일한 쿠폰으로 여러 기기에서 동시에 주문해도, 쿠폰은 단 한 번만 사용된다.")
    @Test
    fun couponIsUsedOnce_underConcurrency() {
        // arrange
        userFacade.signUp(loginId, rawPassword, "홍길동", LocalDate.of(1994, 7, 14), "hong@example.com")
        val userId = userService.authenticate(loginId, rawPassword).id
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 1_000, 100, ProductStatus.ON_SALE)
        val coupon = couponService.register(
            name = "10% 할인",
            discountType = DiscountType.RATE,
            discountValue = 10,
            minOrderAmount = 10_000,
            expiredAt = ZonedDateTime.parse("2026-12-31T23:59:59+09:00"),
        )
        userCouponService.issue(userId, coupon.id)

        val threadCount = 10
        val latch = CountDownLatch(threadCount)
        val executor = Executors.newFixedThreadPool(threadCount)
        val success = AtomicInteger(0)
        val failure = AtomicInteger(0)

        // act: 동일 쿠폰으로 10건 동시 주문 (각 주문 금액 15_000)
        repeat(threadCount) {
            executor.submit {
                try {
                    orderFacade.place(
                        loginId,
                        rawPassword,
                        listOf(OrderFacade.PlaceOrderLine(productId = product.id, quantity = 15)),
                        coupon.id,
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

        // assert: 1건만 성공, 쿠폰은 USED 한 번
        val myCoupon = userCouponService.getMyCoupons(userId, PageRequest.of(0, 10)).content.first()
        assertAll(
            { assertThat(success.get()).isEqualTo(1) },
            { assertThat(failure.get()).isEqualTo(threadCount - 1) },
            { assertThat(myCoupon.status).isEqualTo(CouponStatus.USED) },
        )
    }
}
