package com.loopers.domain.coupon

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.time.ZonedDateTime

class UserCouponModelTest {
    private val notExpired: ZonedDateTime = ZonedDateTime.parse("2026-12-31T23:59:59+09:00")

    private fun coupon(expiredAt: ZonedDateTime = notExpired): CouponModel = CouponModel(
        name = "신규가입 할인",
        discountType = DiscountType.FIXED,
        discountValue = 1_000,
        minOrderAmount = null,
        expiredAt = expiredAt,
    )

    private fun userCoupon(userId: Long = 1L, couponId: Long = 1L): UserCouponModel =
        UserCouponModel(userId = userId, couponId = couponId)

    @DisplayName("발급된 쿠폰을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("기본 상태는 AVAILABLE 이고, 사용 시각은 없다.")
        @Test
        fun createsAvailableUserCoupon() {
            // act
            val result = userCoupon(userId = 1L, couponId = 42L)

            // assert
            assertThat(result.userId).isEqualTo(1L)
            assertThat(result.couponId).isEqualTo(42L)
            assertThat(result.status).isEqualTo(CouponStatus.AVAILABLE)
            assertThat(result.usedAt).isNull()
        }
    }

    @DisplayName("조회 시점의 상태를 판정할 때,")
    @Nested
    inner class StatusAt {
        @DisplayName("사용 가능하고 만료 전이면, AVAILABLE 을 반환한다.")
        @Test
        fun returnsAvailable_whenUsableAndNotExpired() {
            // arrange
            val target = userCoupon()
            val now = ZonedDateTime.parse("2026-06-12T00:00:00+09:00")

            // assert
            assertThat(target.statusAt(coupon(), now)).isEqualTo(CouponStatus.AVAILABLE)
        }

        @DisplayName("만료 시각이 지났으면, EXPIRED 를 반환한다.")
        @Test
        fun returnsExpired_whenCouponExpired() {
            // arrange
            val target = userCoupon()
            val expiredCoupon = coupon(expiredAt = ZonedDateTime.parse("2026-06-01T00:00:00+09:00"))
            val now = ZonedDateTime.parse("2026-06-12T00:00:00+09:00")

            // assert
            assertThat(target.statusAt(expiredCoupon, now)).isEqualTo(CouponStatus.EXPIRED)
        }
    }
}
