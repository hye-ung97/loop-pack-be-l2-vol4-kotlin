package com.loopers.domain.coupon

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.ZonedDateTime

class CouponModelTest {
    private val expiredAt: ZonedDateTime = ZonedDateTime.parse("2026-12-31T23:59:59+09:00")

    private fun coupon(
        name: String = "신규가입 할인",
        discountType: DiscountType = DiscountType.FIXED,
        discountValue: Long = 1_000,
        minOrderAmount: Long? = null,
        expiredAt: ZonedDateTime = this.expiredAt,
    ): CouponModel = CouponModel(
        name = name,
        discountType = discountType,
        discountValue = discountValue,
        minOrderAmount = minOrderAmount,
        expiredAt = expiredAt,
    )

    @DisplayName("쿠폰 템플릿을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 정액(FIXED) 값을 주면, 정상적으로 생성된다.")
        @Test
        fun createsFixedCoupon_whenValuesAreValid() {
            // act
            val result = coupon(discountType = DiscountType.FIXED, discountValue = 1_000, minOrderAmount = 10_000)

            // assert
            assertThat(result.discountType).isEqualTo(DiscountType.FIXED)
            assertThat(result.discountValue).isEqualTo(1_000L)
            assertThat(result.minOrderAmount).isEqualTo(10_000L)
        }

        @DisplayName("유효한 정률(RATE) 값을 주면, 정상적으로 생성된다.")
        @Test
        fun createsRateCoupon_whenValuesAreValid() {
            // act
            val result = coupon(discountType = DiscountType.RATE, discountValue = 10)

            // assert
            assertThat(result.discountType).isEqualTo(DiscountType.RATE)
            assertThat(result.discountValue).isEqualTo(10L)
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            val result = assertThrows<CoreException> { coupon(name = "  ") }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("정률 쿠폰의 할인율이 100을 초과하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenRateExceeds100() {
            val result = assertThrows<CoreException> { coupon(discountType = DiscountType.RATE, discountValue = 101) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("정률 쿠폰의 할인율이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenRateIsNotPositive() {
            val result = assertThrows<CoreException> { coupon(discountType = DiscountType.RATE, discountValue = 0) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("정액 쿠폰의 할인 금액이 0 이하면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenFixedValueIsNotPositive() {
            val result = assertThrows<CoreException> { coupon(discountType = DiscountType.FIXED, discountValue = 0) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("최소 주문 금액이 음수면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenMinOrderAmountIsNegative() {
            val result = assertThrows<CoreException> { coupon(minOrderAmount = -1) }
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("할인 금액을 계산할 때,")
    @Nested
    inner class CalculateDiscount {
        @DisplayName("정액 쿠폰은 할인 금액만큼 할인한다.")
        @Test
        fun fixed_discountsByValue() {
            val target = coupon(discountType = DiscountType.FIXED, discountValue = 1_000)
            assertThat(target.calculateDiscount(10_000)).isEqualTo(1_000L)
        }

        @DisplayName("정액 쿠폰의 할인 금액이 주문 금액보다 크면, 주문 금액까지만 할인한다.")
        @Test
        fun fixed_isCappedAtOrderAmount() {
            val target = coupon(discountType = DiscountType.FIXED, discountValue = 5_000)
            assertThat(target.calculateDiscount(3_000)).isEqualTo(3_000L)
        }

        @DisplayName("정률 쿠폰은 비율만큼 할인하며, 원 단위 미만은 버린다.")
        @Test
        fun rate_discountsByPercentageFloored() {
            val target = coupon(discountType = DiscountType.RATE, discountValue = 10)
            // 10999 * 10 / 100 = 1099.9 -> 1099
            assertThat(target.calculateDiscount(10_999)).isEqualTo(1_099L)
        }

        @DisplayName("최소 주문 금액 조건을 만족하지 않으면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenBelowMinOrderAmount() {
            val target = coupon(minOrderAmount = 10_000)
            val result = assertThrows<CoreException> { target.calculateDiscount(9_999) }
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("최소 주문 금액 조건을 만족하면, 정상적으로 할인 금액을 계산한다.")
        @Test
        fun calculates_whenMeetsMinOrderAmount() {
            val target = coupon(discountType = DiscountType.FIXED, discountValue = 1_000, minOrderAmount = 10_000)
            assertThat(target.calculateDiscount(10_000)).isEqualTo(1_000L)
        }
    }

    @DisplayName("만료 여부를 판단할 때,")
    @Nested
    inner class IsExpired {
        @DisplayName("만료 시각 이전이면, false 를 반환한다.")
        @Test
        fun returnsFalse_whenBeforeExpiry() {
            val target = coupon(expiredAt = ZonedDateTime.parse("2026-12-31T23:59:59+09:00"))
            assertThat(target.isExpired(ZonedDateTime.parse("2026-06-12T00:00:00+09:00"))).isFalse()
        }

        @DisplayName("만료 시각 이후면, true 를 반환한다.")
        @Test
        fun returnsTrue_whenAfterExpiry() {
            val target = coupon(expiredAt = ZonedDateTime.parse("2026-12-31T23:59:59+09:00"))
            assertThat(target.isExpired(ZonedDateTime.parse("2027-01-01T00:00:00+09:00"))).isTrue()
        }
    }
}
