package com.loopers.domain.product

import com.loopers.infrastructure.product.ProductJpaRepository
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.data.domain.PageRequest

@SpringBootTest
class ProductServiceIntegrationTest @Autowired constructor(
    private val productService: ProductService,
    private val productJpaRepository: ProductJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    private fun register(
        brandId: Long = 1L,
        name: String = "Air Max",
        price: Long = 100_000,
        stockQuantity: Int = 10,
        status: ProductStatus = ProductStatus.ON_SALE,
    ): ProductModel = productService.register(brandId, name, price, stockQuantity, status)

    @DisplayName("상품을 등록할 때,")
    @Nested
    inner class Register {
        @DisplayName("유효한 값을 주면, 신규 상품이 저장된다.")
        @Test
        fun savesProduct_whenValid() {
            // act
            val result = register(name = "Air Max", price = 100_000, stockQuantity = 10)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.name).isEqualTo("Air Max") },
                { assertThat(result.stock.quantity).isEqualTo(10) },
            )
        }
    }

    @DisplayName("판매중인 상품을 ID로 조회할 때 (대고객),")
    @Nested
    inner class GetOnSaleById {
        @DisplayName("ON_SALE 상품이면, 반환된다.")
        @Test
        fun returnsProduct_whenOnSale() {
            // arrange
            val saved = register(status = ProductStatus.ON_SALE)

            // act
            val result = productService.getOnSaleById(saved.id)

            // assert
            assertThat(result.id).isEqualTo(saved.id)
        }

        @DisplayName("HIDDEN 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenHidden() {
            // arrange
            val saved = register(status = ProductStatus.HIDDEN)

            // act
            val result = assertThrows<CoreException> { productService.getOnSaleById(saved.id) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("soft delete 된 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenSoftDeleted() {
            // arrange
            val saved = register()
            productService.delete(saved.id)

            // act
            val result = assertThrows<CoreException> { productService.getOnSaleById(saved.id) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenAbsent() {
            // act
            val result = assertThrows<CoreException> { productService.getOnSaleById(999L) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("어드민이 상품을 ID로 조회할 때,")
    @Nested
    inner class GetActiveById {
        @DisplayName("HIDDEN 상품이어도, 반환된다.")
        @Test
        fun returnsProduct_whenHidden() {
            // arrange
            val saved = register(status = ProductStatus.HIDDEN)

            // act
            val result = productService.getActiveById(saved.id)

            // assert
            assertThat(result.id).isEqualTo(saved.id)
        }

        @DisplayName("soft delete 된 상품이면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenSoftDeleted() {
            // arrange
            val saved = register()
            productService.delete(saved.id)

            // act
            val result = assertThrows<CoreException> { productService.getActiveById(saved.id) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("대고객 상품 목록을 조회할 때,")
    @Nested
    inner class GetOnSaleProducts {
        @DisplayName("HIDDEN/soft deleted 상품은 제외하고 ON_SALE 만 반환된다.")
        @Test
        fun returnsOnlyOnSale() {
            // arrange
            val onSale = register(name = "OnSale", status = ProductStatus.ON_SALE)
            register(name = "Hidden", status = ProductStatus.HIDDEN)
            val deleted = register(name = "Deleted", status = ProductStatus.ON_SALE)
            productService.delete(deleted.id)

            // act
            val result = productService.getOnSaleProducts(brandId = null, sort = ProductSort.LATEST, pageable = PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.map { it.id }).containsExactly(onSale.id) },
            )
        }

        @DisplayName("brandId 필터를 주면, 해당 브랜드 상품만 반환된다.")
        @Test
        fun returnsBrandFiltered() {
            // arrange
            val brandA = register(brandId = 1L, name = "A1")
            register(brandId = 2L, name = "B1")
            register(brandId = 1L, name = "A2")

            // act
            val result = productService.getOnSaleProducts(brandId = 1L, sort = ProductSort.LATEST, pageable = PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(2) },
                { assertThat(result.content.map { it.brandId }).containsOnly(1L) },
            )
        }

        @DisplayName("PRICE_ASC 정렬이면, 가격 오름차순으로 반환된다.")
        @Test
        fun returnsPriceAsc() {
            // arrange
            register(name = "Cheap", price = 10_000)
            register(name = "Mid", price = 50_000)
            register(name = "Expensive", price = 100_000)

            // act
            val result = productService.getOnSaleProducts(brandId = null, sort = ProductSort.PRICE_ASC, pageable = PageRequest.of(0, 10))

            // assert
            assertThat(result.content.map { it.price }).containsExactly(10_000L, 50_000L, 100_000L)
        }

        @DisplayName("LIKES_DESC 정렬이면, 좋아요 수 내림차순으로 반환된다.")
        @Test
        fun returnsLikesDesc() {
            // arrange
            val p1 = register(name = "P1")
            val p2 = register(name = "P2")
            val p3 = register(name = "P3")
            // 좋아요 수: p1=1, p2=3, p3=2
            productService.increaseLikeCount(p1.id)
            repeat(3) { productService.increaseLikeCount(p2.id) }
            repeat(2) { productService.increaseLikeCount(p3.id) }

            // act
            val result = productService.getOnSaleProducts(brandId = null, sort = ProductSort.LIKES_DESC, pageable = PageRequest.of(0, 10))

            // assert
            assertThat(result.content.map { it.id }).containsExactly(p2.id, p3.id, p1.id)
        }
    }

    @DisplayName("상품을 수정할 때,")
    @Nested
    inner class Update {
        @DisplayName("name 만 주면, 이름만 변경된다.")
        @Test
        fun changesNameOnly() {
            val saved = register(name = "Old", price = 100_000)
            productService.update(saved.id, name = "New", price = null, status = null)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertAll(
                { assertThat(reloaded.name).isEqualTo("New") },
                { assertThat(reloaded.price).isEqualTo(100_000L) },
            )
        }

        @DisplayName("price/status 도 함께 변경할 수 있다.")
        @Test
        fun changesAllFields() {
            val saved = register(name = "Old", price = 100_000, status = ProductStatus.ON_SALE)
            productService.update(saved.id, name = "New", price = 200_000, status = ProductStatus.HIDDEN)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertAll(
                { assertThat(reloaded.name).isEqualTo("New") },
                { assertThat(reloaded.price).isEqualTo(200_000L) },
                { assertThat(reloaded.status).isEqualTo(ProductStatus.HIDDEN) },
            )
        }

        @DisplayName("존재하지 않으면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenAbsent() {
            val result = assertThrows<CoreException> {
                productService.update(999L, name = "X", price = null, status = null)
            }
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("상품을 삭제할 때,")
    @Nested
    inner class Delete {
        @DisplayName("활성 상품을 삭제하면, soft delete 처리된다.")
        @Test
        fun softDeletes() {
            val saved = register()
            productService.delete(saved.id)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.deletedAt).isNotNull()
        }

        @DisplayName("이미 삭제된 상품을 다시 삭제해도, 예외 없이 처리된다 (멱등).")
        @Test
        fun isIdempotent() {
            val saved = register()
            productService.delete(saved.id)
            productService.delete(saved.id)
        }
    }

    @DisplayName("재고 차감/복구를 할 때,")
    @Nested
    inner class StockOps {
        @DisplayName("deduct: 충분하면 차감된다.")
        @Test
        fun deducts() {
            val saved = register(stockQuantity = 10)
            productService.deductStock(saved.id, 3)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.stock.quantity).isEqualTo(7)
        }

        @DisplayName("deduct: 부족하면 CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenInsufficient() {
            val saved = register(stockQuantity = 2)
            val result = assertThrows<CoreException> { productService.deductStock(saved.id, 3) }
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("restore: 정상 복구된다.")
        @Test
        fun restores() {
            val saved = register(stockQuantity = 5)
            productService.restoreStock(saved.id, 3)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.stock.quantity).isEqualTo(8)
        }
    }

    @DisplayName("좋아요 수를 갱신할 때,")
    @Nested
    inner class LikeCountOps {
        @DisplayName("increase: 1 증가한다.")
        @Test
        fun increases() {
            val saved = register()
            productService.increaseLikeCount(saved.id)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.likeCount).isEqualTo(1L)
        }

        @DisplayName("decrease: 1 감소한다.")
        @Test
        fun decreases() {
            val saved = register()
            productService.increaseLikeCount(saved.id)
            productService.decreaseLikeCount(saved.id)
            val reloaded = productJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.likeCount).isEqualTo(0L)
        }
    }
}
