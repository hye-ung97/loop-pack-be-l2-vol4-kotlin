package com.loopers.interfaces.api.coupon

import com.loopers.application.user.UserFacade
import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.CouponStatus
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.user.UserService
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.ZonedDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userFacade: UserFacade,
    private val userService: UserService,
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

    private fun signUp(): Long {
        userFacade.signUp(loginId, rawPassword, "홍길동", LocalDate.of(1994, 7, 14), "hong@example.com")
        return userService.authenticate(loginId, rawPassword).id
    }

    private fun authHeaders(id: String = loginId, pw: String = rawPassword) = HttpHeaders().apply {
        set("X-Loopers-LoginId", id)
        set("X-Loopers-LoginPw", pw)
    }

    private fun template(name: String = "신규가입 10% 할인") = couponService.register(
        name = name,
        discountType = DiscountType.RATE,
        discountValue = 10,
        minOrderAmount = 10_000,
        expiredAt = ZonedDateTime.parse("2026-12-31T23:59:59+09:00"),
    )

    @DisplayName("POST /api/v1/coupons/{couponId}/issue")
    @Nested
    inner class Issue {
        @DisplayName("유효한 인증과 템플릿 ID로 요청하면, 발급된 쿠폰을 반환한다.")
        @Test
        fun returnsIssuedCoupon_whenValid() {
            // arrange
            signUp()
            val coupon = template()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.MyCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.couponId).isEqualTo(coupon.id) },
                { assertThat(response.body?.data?.status).isEqualTo(CouponStatus.AVAILABLE) },
            )
        }

        @DisplayName("존재하지 않는 템플릿이면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenTemplateDoesNotExist() {
            // arrange
            signUp()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.MyCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/999/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("이미 발급받은 템플릿을 다시 발급하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenAlreadyIssued() {
            // arrange
            val userId = signUp()
            val coupon = template()
            userCouponService.issue(userId, coupon.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.MyCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("잘못된 비밀번호로 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenInvalidCredentials() {
            // arrange
            signUp()
            val coupon = template()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.MyCouponResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/coupons/${coupon.id}/issue",
                HttpMethod.POST,
                HttpEntity<Any>(authHeaders(pw = "Wrong1!pw")),
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("GET /api/v1/users/me/coupons")
    @Nested
    inner class GetMyCoupons {
        @DisplayName("발급받은 쿠폰 목록을 상태와 함께 반환한다.")
        @Test
        fun returnsMyCouponsWithStatus() {
            // arrange
            val userId = signUp()
            val couponA = template("쿠폰 A")
            val couponB = template("쿠폰 B")
            userCouponService.issue(userId, couponA.id)
            userCouponService.issue(userId, couponB.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1Dto.MyCouponPageResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/users/me/coupons",
                HttpMethod.GET,
                HttpEntity<Any>(authHeaders()),
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
                {
                    assertThat(response.body?.data?.content?.map { it.couponId })
                        .containsExactlyInAnyOrder(couponA.id, couponB.id)
                },
                {
                    assertThat(response.body?.data?.content?.map { it.status })
                        .allMatch { it == CouponStatus.AVAILABLE }
                },
            )
        }
    }
}
