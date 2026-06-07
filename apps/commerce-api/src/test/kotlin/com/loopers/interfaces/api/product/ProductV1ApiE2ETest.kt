package com.loopers.interfaces.api.product

import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
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
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ProductV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("GET /api/v1/products/{productId}")
    @Nested
    inner class GetProduct {
        @DisplayName("ON_SALE 상품 ID로 요청하면, 브랜드 정보를 포함한 상품 응답을 반환한다.")
        @Test
        fun returnsProductWithBrand_whenProductIsOnSale() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.id).isEqualTo(product.id) },
                { assertThat(response.body?.data?.name).isEqualTo("Air Max") },
                { assertThat(response.body?.data?.brand?.id).isEqualTo(brand.id) },
                { assertThat(response.body?.data?.brand?.name).isEqualTo("Nike") },
                { assertThat(response.body?.data?.likeCount).isEqualTo(0L) },
            )
        }

        @DisplayName("HIDDEN 상품 ID로 요청하면, 404 NOT_FOUND 응답을 받는다.")
        @Test
        fun returnsNotFound_whenProductIsHidden() {
            // arrange
            val brand = brandService.register("Nike")
            val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.HIDDEN)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductResponse>>() {}
            val response = testRestTemplate.exchange(
                "/api/v1/products/${product.id}",
                HttpMethod.GET,
                HttpEntity.EMPTY,
                responseType,
            )

            // assert
            assertThat(response.statusCode).isEqualTo(HttpStatus.NOT_FOUND)
        }
    }

    @DisplayName("GET /api/v1/products")
    @Nested
    inner class GetProducts {
        @DisplayName("정렬 미지정이면, 최신순(latest)으로 ON_SALE 상품을 반환한다.")
        @Test
        fun returnsLatest_whenSortIsAbsent() {
            // arrange
            val brand = brandService.register("Nike")
            val first = productService.register(brand.id, "First", 10_000, 10, ProductStatus.ON_SALE)
            val second = productService.register(brand.id, "Second", 20_000, 10, ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/products", HttpMethod.GET, HttpEntity.EMPTY, responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.totalElements).isEqualTo(2) },
                { assertThat(response.body?.data?.content?.map { it.id }).containsExactly(second.id, first.id) },
            )
        }

        @DisplayName("sort=price_asc 이면, 가격 오름차순으로 반환한다.")
        @Test
        fun returnsPriceAsc_whenSortIsPriceAsc() {
            // arrange
            val brand = brandService.register("Nike")
            val cheap = productService.register(brand.id, "Cheap", 10_000, 10, ProductStatus.ON_SALE)
            val mid = productService.register(brand.id, "Mid", 50_000, 10, ProductStatus.ON_SALE)
            val expensive = productService.register(brand.id, "Expensive", 100_000, 10, ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/products?sort=price_asc", HttpMethod.GET, HttpEntity.EMPTY, responseType)

            // assert
            assertThat(response.body?.data?.content?.map { it.id }).containsExactly(cheap.id, mid.id, expensive.id)
        }

        @DisplayName("brandId 필터를 주면, 해당 브랜드 상품만 반환한다.")
        @Test
        fun returnsBrandFiltered_whenBrandIdIsGiven() {
            // arrange
            val nike = brandService.register("Nike")
            val adidas = brandService.register("Adidas")
            productService.register(nike.id, "N1", 10_000, 10, ProductStatus.ON_SALE)
            productService.register(adidas.id, "A1", 20_000, 10, ProductStatus.ON_SALE)

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<ProductV1Dto.ProductPageResponse>>() {}
            val response = testRestTemplate.exchange("/api/v1/products?brandId=${nike.id}", HttpMethod.GET, HttpEntity.EMPTY, responseType)

            // assert
            assertAll(
                { assertThat(response.body?.data?.totalElements).isEqualTo(1) },
                { assertThat(response.body?.data?.content?.first()?.name).isEqualTo("N1") },
            )
        }
    }
}
