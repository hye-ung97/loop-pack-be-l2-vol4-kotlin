package com.loopers.interfaces.api.admin.product

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
class ProductV1AdminApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/products"
        private const val ADMIN_LOGIN_ID = "loopersadmin"
    }

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

    @DisplayName("POST /api-admin/v1/products")
    @Nested
    inner class Create {
        @DisplayName("유효한 brandId 와 값으로 요청하면, 등록된 상품 정보를 반환한다.")
        @Test
        fun returnsProduct_whenValid() {
            // arrange
            val brand = brandService.register("Nike")
            val request = ProductV1AdminDto.CreateRequest(
                brandId = brand.id,
                name = "Air Max",
                price = 100_000,
                stockQuantity = 10,
                status = ProductStatus.ON_SALE,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.name).isEqualTo("Air Max") },
                { assertThat(response.body?.data?.brand?.id).isEqualTo(brand.id) },
            )
        }

        @DisplayName("존재하지 않는 brandId 로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandDoesNotExist() {
            // arrange
            val request = ProductV1AdminDto.CreateRequest(
                brandId = 999L,
                name = "Air Max",
                price = 100_000,
                stockQuantity = 10,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }

        @DisplayName("이름이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val brand = brandService.register("Nike")
            val request = ProductV1AdminDto.CreateRequest(
                brandId = brand.id,
                name = "  ",
                price = 100_000,
                stockQuantity = 10,
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api-admin/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("HIDDEN 상품이어도, 어드민 조회로 반환된다.")
        @Test
        fun returnsHiddenProduct() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Hidden", 50_000, 10, ProductStatus.HIDDEN)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${product.id}", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.status).isEqualTo(ProductStatus.HIDDEN) },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/products/{productId}")
    @Nested
    inner class Update {
        @DisplayName("name 만 주면, 이름만 변경된다.")
        @Test
        fun updatesNameOnly() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Old", 100_000, 10, ProductStatus.ON_SALE)
            val request = ProductV1AdminDto.UpdateRequest(name = "New")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1AdminDto.ProductResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${product.id}", HttpMethod.PUT, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("New") },
                { assertThat(response.body?.data?.price).isEqualTo(100_000L) },
            )
        }
    }

    @DisplayName("DELETE /api-admin/v1/products/{productId}")
    @Nested
    inner class Delete {
        @DisplayName("활성 상품을 삭제하면, 200 응답을 받는다.")
        @Test
        fun returnsOk() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Old", 100_000, 10, ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${product.id}", HttpMethod.DELETE, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }
    }
}
