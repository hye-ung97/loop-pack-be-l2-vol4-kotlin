package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductStatus
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductFacadeCacheTest @Autowired constructor(
    private val productFacade: ProductFacade,
    private val productCacheStore: ProductCacheStore,
    private val brandService: BrandService,
    private val productService: ProductService,
    private val databaseCleanUp: DatabaseCleanUp,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
        redisCleanUp.truncateAll()
    }

    @DisplayName("상품 상세를 처음 조회(캐시 미스)하면, 조회 결과가 캐시에 적재된다.")
    @Test
    fun populatesCacheOnMiss() {
        // arrange
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        assertThat(productCacheStore.getProductDetail(product.id)).isNull()

        // act
        val result = productFacade.getProductDetail(product.id)

        // assert
        assertThat(productCacheStore.getProductDetail(product.id)).isEqualTo(result)
    }

    @DisplayName("캐시에 값이 있으면(캐시 히트), DB 가 아닌 캐시의 값을 반환한다.")
    @Test
    fun readsFromCacheOnHit() {
        // arrange
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        // DB 와 다른 값을 캐시에 직접 심어 둔다 (캐시에서 읽었음을 식별하기 위함)
        val cached = ProductInfo(
            id = product.id,
            name = "CACHED-NAME",
            price = 999,
            stockQuantity = 1,
            status = ProductStatus.ON_SALE,
            likeCount = 77,
            brand = BrandInfo(id = brand.id, name = brand.name),
        )
        productCacheStore.setProductDetail(product.id, cached)

        // act
        val result = productFacade.getProductDetail(product.id)

        // assert: DB 값("Air Max") 이 아니라 캐시 값("CACHED-NAME") 이 반환된다
        assertThat(result.name).isEqualTo("CACHED-NAME")
        assertThat(result.likeCount).isEqualTo(77L)
    }
}
