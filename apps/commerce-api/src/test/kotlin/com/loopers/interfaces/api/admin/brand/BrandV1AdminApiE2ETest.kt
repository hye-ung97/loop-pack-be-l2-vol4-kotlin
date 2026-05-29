package com.loopers.interfaces.api.admin.brand

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.user.UserModel
import com.loopers.domain.user.UserRole
import com.loopers.infrastructure.product.ProductJpaRepository
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
class BrandV1AdminApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT = "/api-admin/v1/brands"
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

    @DisplayName("POST /api-admin/v1/brands")
    @Nested
    inner class Create {
        @DisplayName("유효한 이름으로 요청하면, 등록된 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrand_whenValidNameIsProvided() {
            // arrange
            val request = BrandV1AdminDto.CreateRequest(name = "Nike")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isNotNull() },
                { assertThat(response.body?.data?.name).isEqualTo("Nike") },
            )
        }

        @DisplayName("이미 활성 상태로 동일한 이름이 존재하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenActiveBrandWithSameNameExists() {
            // arrange
            brandService.register("Nike")
            val request = BrandV1AdminDto.CreateRequest(name = "Nike")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT)
        }

        @DisplayName("이름이 비어있으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenNameIsBlank() {
            // arrange
            val request = BrandV1AdminDto.CreateRequest(name = "   ")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.POST, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST)
        }
    }

    @DisplayName("GET /api-admin/v1/brands/{brandId}")
    @Nested
    inner class GetBrand {
        @DisplayName("존재하는 브랜드 ID로 요청하면, 브랜드 정보를 반환한다.")
        @Test
        fun returnsBrand_whenBrandExists() {
            // arrange
            val saved = brandService.register("Nike")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(saved.id) },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandDoesNotExist() {
            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/999", HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api-admin/v1/brands")
    @Nested
    inner class GetBrands {
        @DisplayName("기본 페이징(page=0, size=20)으로 요청하면, 활성 브랜드 목록을 반환한다.")
        @Test
        fun returnsActiveBrandPage_whenDefaultPaging() {
            // arrange
            brandService.register("Nike")
            brandService.register("Adidas")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandPageResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT, HttpMethod.GET, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
                { assertThat(response.body?.data?.content?.map { it.name }).containsExactlyInAnyOrder("Nike", "Adidas") },
            )
        }
    }

    @DisplayName("PUT /api-admin/v1/brands/{brandId}")
    @Nested
    inner class Update {
        @DisplayName("유효한 새 이름으로 요청하면, 변경된 브랜드 정보를 반환한다.")
        @Test
        fun returnsUpdatedBrand_whenValidNameIsProvided() {
            // arrange
            val saved = brandService.register("Nike")
            val request = BrandV1AdminDto.UpdateRequest(name = "Adidas")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.PUT, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.name).isEqualTo("Adidas") },
            )
        }

        @DisplayName("존재하지 않는 브랜드 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenBrandDoesNotExist() {
            // arrange
            val request = BrandV1AdminDto.UpdateRequest(name = "Adidas")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/999", HttpMethod.PUT, HttpEntity(request, adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("Admin 인증")
    @Nested
    inner class AdminAuth {
        @DisplayName("X-Loopers-Ldap 헤더가 없으면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLdapHeaderIsMissing() {
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/1", HttpMethod.GET, HttpEntity.EMPTY, responseType)

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("어드민이 아닌 사용자의 loginId로 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenUserIsNotAdmin() {
            // arrange
            userJpaRepository.save(
                UserModel(
                    loginId = "normaluser",
                    password = "encoded",
                    name = "일반",
                    birthDate = LocalDate.of(1990, 1, 1),
                    email = "u@example.com",
                    role = UserRole.USER,
                ),
            )
            val headers = HttpHeaders().apply { set("X-Loopers-Ldap", "normaluser") }

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/1", HttpMethod.GET, HttpEntity<Any>(headers), responseType)

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }

        @DisplayName("존재하지 않는 loginId로 요청하면, 401 UNAUTHORIZED 응답을 받는다.")
        @Test
        fun returnsUnauthorized_whenLoginIdDoesNotExist() {
            val headers = HttpHeaders().apply { set("X-Loopers-Ldap", "ghost") }
            val responseType = object : ParameterizedTypeReference<ApiResponse<BrandV1AdminDto.BrandResponse>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/1", HttpMethod.GET, HttpEntity<Any>(headers), responseType)

            assertThat(response.statusCode).isEqualTo(HttpStatus.UNAUTHORIZED)
        }
    }

    @DisplayName("DELETE /api-admin/v1/brands/{brandId}")
    @Nested
    inner class Delete {
        @DisplayName("활성 브랜드를 삭제하면, 200 응답을 받는다.")
        @Test
        fun returnsOk_whenBrandIsActive() {
            // arrange
            val saved = brandService.register("Nike")

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.DELETE, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제해도, 200 응답을 받는다 (멱등).")
        @Test
        fun returnsOk_whenBrandIsAlreadyDeleted() {
            // arrange
            val saved = brandService.register("Nike")
            brandService.delete(saved.id)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${saved.id}", HttpMethod.DELETE, HttpEntity<Any>(adminHeaders()), responseType)

            // assert
            assertThat(response.statusCode.is2xxSuccessful).isTrue()
        }

        @DisplayName("브랜드 삭제 시 해당 브랜드의 활성 상품도 함께 soft delete 된다 (cascade).")
        @Test
        fun cascadeSoftDeletesProducts() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Air Max", 100_000, 10, com.loopers.domain.product.ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<Unit>>() {}
            val response = testRestTemplate.exchange("$ENDPOINT/${brand.id}", HttpMethod.DELETE, HttpEntity<Any>(adminHeaders()), responseType)

            // assert: 상품이 soft delete 상태가 된다
            val reloaded = productJpaRepository.findById(product.id).orElseThrow()
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(reloaded.deletedAt).isNotNull() },
            )
        }
    }
}
