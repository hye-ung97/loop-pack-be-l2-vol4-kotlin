package com.loopers.interfaces.api.admin.coupon

import com.loopers.domain.coupon.CouponService
import com.loopers.domain.coupon.DiscountType
import com.loopers.domain.coupon.UserCouponService
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserRole
import com.loopers.infrastructure.user.UserJpaRepository
import com.loopers.interfaces.api.ApiResponse
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
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
import java.time.LocalDateTime

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class CouponV1AdminApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val couponService: CouponService,
    private val userCouponService: UserCouponService,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/coupons"
        private const val ADMIN_LOGIN_ID = "loopersadmin"
    }

    private val expiredAt = LocalDateTime.parse("2026-12-31T23:59:59")

    private fun adminHeaders() = HttpHeaders().apply { set("X-Loopers-Ldap", ADMIN_LOGIN_ID) }

    private fun registerTemplate(name: String = "신규가입 10% 할인") = couponService.register(
        name = name,
        discountType = DiscountType.RATE,
        discountValue = 10,
        minOrderAmount = 10_000,
        expiredAt = java.time.ZonedDateTime.parse("2026-12-31T23:59:59+09:00"),
    )

    @BeforeEach
    fun setUpAdmin() {
        userJpaRepository.save(
            UserModel(
                loginId = ADMIN_LOGIN_ID,
                password = "encoded",
                name = "관리자",
                birthDate = LocalDate.of(1990, 1, 1),
                email = "admin@example.com",
                role = UserRole.ADMIN,
            ),
        )
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api-admin/v1/coupons")
    @Nested
    inner class Create {
        @DisplayName("유효한 정률 쿠폰 템플릿을 등록하면, 등록된 정보를 반환한다.")
        @Test
        fun returnsCoupon_whenValidRateTemplate() {
            // arrange
            val request = CouponV1AdminDto.CreateRequest(
                name = "신규가입 10% 할인",
                type = DiscountType.RATE,
                value = 10,
                minOrderAmount = 10_000,
                expiredAt = expiredAt,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.type).isEqualTo(DiscountType.RATE) },
                { assertThat(response.body?.data?.value).isEqualTo(10L) },
            )
        }

        @DisplayName("정률 할인율이 100을 초과하면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenRateExceeds100() {
            // arrange
            val request = CouponV1AdminDto.CreateRequest(
                name = "잘못된 쿠폰",
                type = DiscountType.RATE,
                value = 101,
                minOrderAmount = null,
                expiredAt = expiredAt,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class GetCoupon {
        @DisplayName("존재하는 템플릿 ID로 요청하면, 템플릿 정보를 반환한다.")
        @Test
        fun returnsCoupon_whenExists() {
            // arrange
            val saved = registerTemplate()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(saved.id) },
            )
        }

        @DisplayName("존재하지 않는 템플릿 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenDoesNotExist() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/999", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/coupons")
    @Nested
    inner class GetCoupons {
        @DisplayName("기본 페이징으로 요청하면, 활성 템플릿 목록을 반환한다.")
        @Test
        fun returnsActiveCouponPage() {
            // arrange
            registerTemplate("쿠폰 A")
            registerTemplate("쿠폰 B")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponPageResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class Update {
        @DisplayName("유효한 값으로 수정하면, 변경된 템플릿 정보를 반환한다.")
        @Test
        fun returnsUpdatedCoupon() {
            // arrange
            val saved = registerTemplate("기존 쿠폰")
            val request = CouponV1AdminDto.UpdateRequest(
                name = "수정된 쿠폰",
                type = DiscountType.FIXED,
                value = 2_000,
                minOrderAmount = 5_000,
                expiredAt = expiredAt,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.PUT, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("수정된 쿠폰") },
                { assertThat(response.body?.data?.type).isEqualTo(DiscountType.FIXED) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/coupons/{couponId}")
    @Nested
    inner class Delete {
        @DisplayName("활성 템플릿을 삭제하면, 200 응답을 받는다.")
        @Test
        fun returnsOk_whenActive() {
            // arrange
            val saved = registerTemplate()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.DELETE, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }
    }

    @DisplayName("GET /api-admin/v1/coupons/{couponId}/issues")
    @Nested
    inner class GetIssues {
        @DisplayName("특정 쿠폰의 발급 내역을 페이징하여 반환한다.")
        @Test
        fun returnsIssuePage() {
            // arrange
            val template = registerTemplate()
            userCouponService.issue(userId = 1L, couponId = template.id)
            userCouponService.issue(userId = 2L, couponId = template.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.IssuePageResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${template.id}/issues", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
                { assertThat(response.body?.data?.content?.map { it.userId }).containsExactlyInAnyOrder(1L, 2L) },
            )
        }
    }

    @DisplayName("X-Loopers-Ldap 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
    @Test
    fun returnsUnauthorized_whenLdapHeaderIsMissing() {
        val responseType = object : ParameterizedTypeReference<ApiResponse<CouponV1AdminDto.CouponResponse>>() {}
        val response = testRestTemplate.exchange("$ENDPOINT/1", HttpMethod.GET, HttpEntity.EMPTY, responseType)
        assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
    }
}
