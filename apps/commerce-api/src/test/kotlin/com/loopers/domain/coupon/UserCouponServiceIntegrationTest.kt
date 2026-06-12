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
