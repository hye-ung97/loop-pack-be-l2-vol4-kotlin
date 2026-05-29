package com.loopers.interfaces.api.admin.order

import com.loopers.application.order.OrderFacade
import com.loopers.application.user.UserFacade
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
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

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class OrderV1AdminApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val userFacade: UserFacade,
    private val orderFacade: OrderFacade,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/orders"
        private const val ADMIN_LOGIN_ID = "loopersadmin"
    }

    private val loginId = "user123"
    private val rawPassword = "Valid1!pw"

    private fun adminHeaders() = HttpHeaders().apply {
        set("X-Loopers-Ldap", ADMIN_LOGIN_ID)
    }

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

    private fun placeUserOrder(): Long {
        userFacade.signUp(loginId, rawPassword, "홍길동", LocalDate.of(1994, 7, 14), "hong@example.com")
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "P", 1_000, 10, ProductStatus.ON_SALE)
        val order = orderFacade.place(
            loginId,
            rawPassword,
            listOf(OrderFacade.PlaceOrderLine(product.id, 2)),
        )
        return order.id
    }

    @DisplayName("GET /api-admin/v1/orders/{orderId}")
    @Nested
    inner class GetOrder {
        @DisplayName("존재하는 주문이면, 상세를 반환한다.")
        @Test
        fun returnsOrder() {
            // arrange
            val orderId = placeUserOrder()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1AdminDto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/$orderId", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(orderId) },
            )
        }

        @DisplayName("존재하지 않으면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1AdminDto.OrderResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/999", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/orders")
    @Nested
    inner class GetOrders {
        @DisplayName("전체 주문 목록이 반환된다.")
        @Test
        fun returnsAllOrders() {
            // arrange
            placeUserOrder()

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<OrderV1AdminDto.OrderPageResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(1) },
            )
        }
    }
}
