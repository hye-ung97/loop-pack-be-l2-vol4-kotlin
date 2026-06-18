package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.product.ProductStatus
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest

@SpringBootTest
class ProductCacheStoreTest @Autowired constructor(
    private val productCacheStore: ProductCacheStore,
    private val redisCleanUp: RedisCleanUp,
) {
    @AfterEach
    fun tearDown() {
        redisCleanUp.truncateAll()
    }

    private fun productInfo(id: Long = 1L): ProductInfo = ProductInfo(
        id = id,
        name = "Air Max",
        price = 100_000,
        stockQuantity = 10,
        status = ProductStatus.ON_SALE,
        likeCount = 5,
        brand = BrandInfo(id = 1L, name = "Nike"),
    )

    @DisplayName("상품 상세를 저장한 뒤 조회하면, 저장한 값이 그대로 반환된다.")
    @Test
    fun setThenGetProductDetail() {
        // arrange
        val info = productInfo()

        // act
        productCacheStore.setProductDetail(info.id, info)
        val cached = productCacheStore.getProductDetail(info.id)

        // assert
        assertThat(cached).isEqualTo(info)
    }

    @DisplayName("캐시에 없는 상품 상세를 조회하면, null 을 반환한다.")
    @Test
    fun getProductDetail_whenMiss_returnsNull() {
        // act
        val cached = productCacheStore.getProductDetail(999L)

        // assert
        assertThat(cached).isNull()
    }

    @DisplayName("상품 상세 캐시를 무효화하면, 이후 조회 시 null 이 반환된다.")
    @Test
    fun evictProductDetail() {
        // arrange
        val info = productInfo()
        productCacheStore.setProductDetail(info.id, info)

        // act
        productCacheStore.evictProductDetail(info.id)

        // assert
        assertThat(productCacheStore.getProductDetail(info.id)).isNull()
    }
}
