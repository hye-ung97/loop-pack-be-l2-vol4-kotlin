package com.loopers.application.product

import com.loopers.application.brand.BrandInfo
import com.loopers.domain.brand.BrandService
import com.loopers.domain.product.ProductService
import com.loopers.domain.product.ProductSort
import com.loopers.domain.product.ProductStatus
import com.loopers.utils.DatabaseCleanUp
import com.loopers.utils.RedisCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ProductListCacheTest @Autowired constructor(
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

    @DisplayName("상품 목록을 처음 조회(캐시 미스)하면, 조회 결과가 캐시에 적재된다.")
    @Test
    fun populatesListCacheOnMiss() {
        // arrange
        val brand = brandService.register("Nike")
        productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        productService.register(brand.id, "Pegasus", 90_000, 10, ProductStatus.ON_SALE)
        val pageable = PageRequest.of(0, 20)
        assertThat(productCacheStore.getProductList(brand.id, ProductSort.LATEST, pageable)).isNull()

        // act
        val result = productFacade.getProducts(brand.id, ProductSort.LATEST, pageable)

        // assert
        val cached = productCacheStore.getProductList(brand.id, ProductSort.LATEST, pageable)
        assertThat(cached).isNotNull
        assertThat(cached!!.content).isEqualTo(result.content)
        assertThat(cached.totalElements).isEqualTo(result.totalElements)
    }

    @DisplayName("목록 캐시에 값이 있으면(캐시 히트), DB 가 아닌 캐시의 값을 반환한다.")
    @Test
    fun readsListFromCacheOnHit() {
        // arrange
        val brand = brandService.register("Nike")
        val product = productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        val pageable = PageRequest.of(0, 20)
        // DB 와 다른 값을 캐시에 직접 심어 둔다
        val cachedInfo = ProductInfo(
            id = product.id,
            name = "CACHED-LIST-ITEM",
            price = 1,
            stockQuantity = 1,
            status = ProductStatus.ON_SALE,
            likeCount = 0,
            brand = BrandInfo(id = brand.id, name = brand.name),
        )
        productCacheStore.setProductList(
            brand.id,
            ProductSort.LATEST,
            pageable,
            CachedProductPage(content = listOf(cachedInfo), totalElements = 1),
        )

        // act
        val result = productFacade.getProducts(brand.id, ProductSort.LATEST, pageable)

        // assert
        assertThat(result.content).hasSize(1)
        assertThat(result.content[0].name).isEqualTo("CACHED-LIST-ITEM")
    }

    @DisplayName("정렬 조건이 다르면, 캐시 키가 분리되어 서로 다른 캐시 항목으로 취급된다.")
    @Test
    fun cacheKeyIncludesSort() {
        // arrange
        val brand = brandService.register("Nike")
        productService.register(brand.id, "Air Max", 100_000, 10, ProductStatus.ON_SALE)
        val pageable = PageRequest.of(0, 20)

        // act: LIKES_DESC 로만 조회해 캐시 적재
        productFacade.getProducts(brand.id, ProductSort.LIKES_DESC, pageable)

        // assert: 같은 조건의 LIKES_DESC 는 캐시 적재됨, 다른 정렬(PRICE_ASC)은 별도 키라 미적재(null)
        assertThat(productCacheStore.getProductList(brand.id, ProductSort.LIKES_DESC, pageable)).isNotNull
        assertThat(productCacheStore.getProductList(brand.id, ProductSort.PRICE_ASC, pageable)).isNull()
    }
}
