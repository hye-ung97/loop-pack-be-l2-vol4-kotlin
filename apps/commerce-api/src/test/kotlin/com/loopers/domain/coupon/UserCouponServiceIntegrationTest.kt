package com.loopers.domain.coupon

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
import java.time.ZonedDateTime

@SpringBootTest
class UserCouponServiceIntegrationTest @Autowired constructor(
    private val userCouponService: UserCouponService,
    private val couponService: CouponService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val expiredAt: ZonedDateTime = ZonedDateTime.parse("2026-12-31T23:59:59+09:00")

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun coupon(name: String = "신규가입 10% 할인"): CouponModel = couponService.register(
        name = name,
        discountType = DiscountType.RATE,
        discountValue = 10,
        minOrderAmount = 10_000,
        expiredAt = expiredAt,
    )

    @DisplayName("쿠폰을 발급할 때,")
    @Nested
    inner class Issue {
        @DisplayName("유효한 템플릿이면, 사용 가능한 쿠폰이 발급된다.")
        @Test
        fun issuesAvailableCoupon_whenTemplateIsValid() {
            // arrange
            val template = coupon()

            // act
            val result = userCouponService.issue(userId = 1L, couponId = template.id)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.userId).isEqualTo(1L) },
                { assertThat(result.couponId).isEqualTo(template.id) },
                { assertThat(result.status).isEqualTo(CouponStatus.AVAILABLE) },
            )
        }

        @DisplayName("존재하지 않는 템플릿이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenTemplateDoesNotExist() {
            val result = assertThrows<CoreException> { userCouponService.issue(userId = 1L, couponId = 999L) }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 발급받은 템플릿을 다시 발급하면, CONFLICT 예외가 발생한다. (유저당 1장)")
        @Test
        fun throwsConflict_whenAlreadyIssued() {
            // arrange
            val template = coupon()
            userCouponService.issue(userId = 1L, couponId = template.id)

            // act
            val result = assertThrows<CoreException> { userCouponService.issue(userId = 1L, couponId = template.id) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("다른 유저는 같은 템플릿을 각각 발급받을 수 있다.")
        @Test
        fun allowsDifferentUsersToIssueSameTemplate() {
            // arrange
            val template = coupon()

            // act
            val first = userCouponService.issue(userId = 1L, couponId = template.id)
            val second = userCouponService.issue(userId = 2L, couponId = template.id)

            // assert
            assertThat(first.id).isNotEqualTo(second.id)
        }
    }

    @DisplayName("쿠폰을 사용할 때,")
    @Nested
    inner class Use {
        private val now: ZonedDateTime = ZonedDateTime.parse("2026-06-12T00:00:00+09:00")

        @DisplayName("보유한 사용 가능 쿠폰이면, 할인액을 반환하고 USED 로 전이된다.")
        @Test
        fun returnsDiscountAndMarksUsed_whenUsable() {
            // arrange
            val template = coupon() // RATE 10%, minOrderAmount 10_000
            val issued = userCouponService.issue(userId = 1L, couponId = template.id)

            // act
            val discount = userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 20_000, now = now)

            // assert
            assertThat(discount).isEqualTo(2_000L)
            val reloaded = userCouponService.getMyCoupons(1L, PageRequest.of(0, 10)).content.first { it.id == issued.id }
            assertThat(reloaded.status).isEqualTo(CouponStatus.USED)
        }

        @DisplayName("보유하지 않은(타 유저 소유 포함) 쿠폰이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenNotOwned() {
            // arrange
            val template = coupon()
            userCouponService.issue(userId = 2L, couponId = template.id)

            // act
            val result = assertThrows<CoreException> {
                userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 20_000, now = now)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("이미 사용한 쿠폰이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenAlreadyUsed() {
            // arrange
            val template = coupon()
            userCouponService.issue(userId = 1L, couponId = template.id)
            userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 20_000, now = now)

            // act
            val result = assertThrows<CoreException> {
                userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 20_000, now = now)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("만료된 쿠폰이면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenExpired() {
            // arrange
            val template = coupon()
            userCouponService.issue(userId = 1L, couponId = template.id)
            val afterExpiry = ZonedDateTime.parse("2027-01-01T00:00:00+09:00")

            // act
            val result = assertThrows<CoreException> {
                userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 20_000, now = afterExpiry)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("최소 주문 금액 조건을 만족하지 않으면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenBelowMinOrderAmount() {
            // arrange
            val template = coupon() // minOrderAmount 10_000
            userCouponService.issue(userId = 1L, couponId = template.id)

            // act
            val result = assertThrows<CoreException> {
                userCouponService.use(userId = 1L, couponId = template.id, orderAmount = 9_999, now = now)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("내 쿠폰 목록을 조회할 때,")
    @Nested
    inner class GetMyCoupons {
        @DisplayName("해당 유저가 발급받은 쿠폰만 페이징되어 반환된다.")
        @Test
        fun returnsOnlyMyCoupons() {
            // arrange
            val templateA = coupon(name = "쿠폰 A")
            val templateB = coupon(name = "쿠폰 B")
            userCouponService.issue(userId = 1L, couponId = templateA.id)
            userCouponService.issue(userId = 1L, couponId = templateB.id)
            userCouponService.issue(userId = 2L, couponId = templateA.id)

            // act
            val result = userCouponService.getMyCoupons(userId = 1L, pageable = PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(2) },
                { assertThat(result.content.map { it.couponId }).containsExactlyInAnyOrder(templateA.id, templateB.id) },
            )
        }
    }
}
