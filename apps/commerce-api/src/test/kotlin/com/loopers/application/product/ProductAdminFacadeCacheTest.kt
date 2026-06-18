package com.loopers.application.product

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
class ProductAdminFacadeCacheTest @Autowired constructor(
    private val productAdminFacade: ProductAdminFacade,
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

    @DisplayName("상품을 수정하면, 해당 상품의 상세 캐시가 무효화된다.")
    @Test
    fun update_evictsDetailCache() {
        // arrange
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        productFacade.getProductDetail(product.id) // 캐시 워밍
        assertThat(productCacheStore.getProductDetail(product.id)).isNotNull

        // act
        productAdminFacade.update(product.id, "Air Max 2", null, null)

        // assert
        assertThat(productCacheStore.getProductDetail(product.id)).isNull()
    }

    @DisplayName("상품을 삭제하면, 해당 상품의 상세 캐시가 무효화된다.")
    @Test
    fun delete_evictsDetailCache() {
        // arrange
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        productFacade.getProductDetail(product.id) // 캐시 워밍
        assertThat(productCacheStore.getProductDetail(product.id)).isNotNull

        // act
        productAdminFacade.delete(product.id)

        // assert
        assertThat(productCacheStore.getProductDetail(product.id)).isNull()
    }
}
