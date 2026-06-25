package com.loopers.application.product

import com.fasterxml.jackson.databind.ObjectMapper
import com.loopers.domain.product.ProductSort
import org.slf4j.LoggerFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Component
import java.time.Duration

/**
 * 목록 캐시는 Spring [org.springframework.data.domain.Page] 를 직접 직렬화하지 않고,
 * 재구성에 필요한 content 와 totalElements 만 저장한다.
 */
data class CachedProductPage(
    val content: List<ProductInfo>,
    val totalElements: Long,
)

/**
 * 상품 조회 결과를 Redis 에 Look-aside 방식으로 캐싱한다.
 *
 * 캐시는 보조 수단이므로, Redis 장애/직렬화 실패 시에도 예외를 전파하지 않고
 * (조회는 null, 저장/무효화는 무시) 호출 측이 DB 로 폴백할 수 있도록 한다.
 */
@Component
class ProductCacheStore(
    redisTemplate: RedisTemplate<*, *>,
    private val objectMapper: ObjectMapper,
) {
    @Suppress("UNCHECKED_CAST")
    private val redis = redisTemplate as RedisTemplate<String, String>

    fun getProductDetail(productId: Long): ProductInfo? =
        runCatching {
            redis.opsForValue().get(detailKey(productId))
                ?.let { objectMapper.readValue(it, ProductInfo::class.java) }
        }.getOrElse {
            log.warn("상품 상세 캐시 조회 실패. productId={}", productId, it)
            null
        }

    fun setProductDetail(productId: Long, info: ProductInfo) {
        runCatching {
            redis.opsForValue().set(detailKey(productId), objectMapper.writeValueAsString(info), DETAIL_TTL)
        }.onFailure { log.warn("상품 상세 캐시 저장 실패. productId={}", productId, it) }
    }

    fun evictProductDetail(productId: Long) {
        runCatching {
            redis.delete(detailKey(productId))
        }.onFailure { log.warn("상품 상세 캐시 무효화 실패. productId={}", productId, it) }
    }

    fun getProductList(brandId: Long?, sort: ProductSort, pageable: Pageable): CachedProductPage? =
        runCatching {
            redis.opsForValue().get(listKey(brandId, sort, pageable))
                ?.let { objectMapper.readValue(it, CachedProductPage::class.java) }
        }.getOrElse {
            log.warn("상품 목록 캐시 조회 실패. brandId={}, sort={}", brandId, sort, it)
            null
        }

    fun setProductList(brandId: Long?, sort: ProductSort, pageable: Pageable, page: CachedProductPage) {
        runCatching {
            redis.opsForValue().set(listKey(brandId, sort, pageable), objectMapper.writeValueAsString(page), LIST_TTL)
        }.onFailure { log.warn("상품 목록 캐시 저장 실패. brandId={}, sort={}", brandId, sort, it) }
    }

    private fun detailKey(productId: Long): String = "$DETAIL_KEY_PREFIX$productId"

    private fun listKey(brandId: Long?, sort: ProductSort, pageable: Pageable): String =
        "$LIST_KEY_PREFIX${brandId ?: "all"}:${sort.name}:${pageable.pageNumber}:${pageable.pageSize}"

    companion object {
        private val log = LoggerFactory.getLogger(ProductCacheStore::class.java)
        private const val DETAIL_KEY_PREFIX = "product:detail:"
        private const val LIST_KEY_PREFIX = "product:list:"
        private val DETAIL_TTL: Duration = Duration.ofMinutes(10)
        private val LIST_TTL: Duration = Duration.ofMinutes(1)
    }
}
