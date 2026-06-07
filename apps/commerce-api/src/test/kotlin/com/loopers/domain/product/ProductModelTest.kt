package com.loopers.domain.product

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows

class ProductModelTest {
    private fun product(
        brandId: Long = 1L,
        name: String = "Air Max",
        price: Long = 100_000,
        stockQuantity: Int = 10,
        status: ProductStatus = ProductStatus.ON_SALE,
    ): ProductModel = ProductModel(
        brandId = brandId,
        name = name,
        price = price,
        stock = Stock(stockQuantity),
        status = status,
    )

    @DisplayName("상품 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 값을 주면, 정상적으로 생성된다.")
        @Test
        fun createsProductModel_whenAllValuesAreValid() {
            // act
            val result = product(brandId = 1L, name = "Air Max", price = 100_000, stockQuantity = 5)

            // assert
            assertAll(
                { assertThat(result.brandId).isEqualTo(1L) },
                { assertThat(result.name).isEqualTo("Air Max") },
                { assertThat(result.price).isEqualTo(100_000L) },
                { assertThat(result.stock.quantity).isEqualTo(5) },
                { assertThat(result.status).isEqualTo(ProductStatus.ON_SALE) },
                { assertThat(result.likeCount).isEqualTo(0L) },
            )
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // act
            val result = assertThrows<CoreException> { product(name = "  ") }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("가격이 음수면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPriceIsNegative() {
            // act
            val result = assertThrows<CoreException> { product(price = -1) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("재고가 음수면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenStockIsNegative() {
            // act
            val result = assertThrows<CoreException> { Stock(-1) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("재고를 차감할 때,")
    @Nested
    inner class Deduct {
        @DisplayName("충분한 재고가 있으면, 차감된다.")
        @Test
        fun deductsStock_whenStockIsEnough() {
            // arrange
            val target = product(stockQuantity = 10)

            // act
            target.deduct(3)

            // assert
            assertThat(target.stock.quantity).isEqualTo(7)
        }

        @DisplayName("재고가 부족하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenStockIsInsufficient() {
            // arrange
            val target = product(stockQuantity = 2)

            // act
            val result = assertThrows<CoreException> { target.deduct(3) }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }
    }

    @DisplayName("재고를 복구할 때,")
    @Nested
    inner class RestoreStock {
        @DisplayName("정상 복구되어 수량이 증가한다.")
        @Test
        fun increasesStock_whenRestored() {
            // arrange
            val target = product(stockQuantity = 5)

            // act
            target.restoreStock(3)

            // assert
            assertThat(target.stock.quantity).isEqualTo(8)
        }
    }

    @DisplayName("좋아요 수를 변경할 때,")
    @Nested
    inner class LikeCount {
        @DisplayName("증가시키면, 1 증가한다.")
        @Test
        fun increasesLikeCount_whenIncreased() {
            // arrange
            val target = product()

            // act
            target.increaseLikeCount()

            // assert
            assertThat(target.likeCount).isEqualTo(1L)
        }

        @DisplayName("감소시키면, 1 감소한다.")
        @Test
        fun decreasesLikeCount_whenDecreased() {
            // arrange
            val target = product().also {
                it.increaseLikeCount()
                it.increaseLikeCount()
            }

            // act
            target.decreaseLikeCount()

            // assert
            assertThat(target.likeCount).isEqualTo(1L)
        }

        @DisplayName("0인 상태에서 감소시키면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenDecreasingFromZero() {
            // arrange
            val target = product()

            // act
            val result = assertThrows<CoreException> { target.decreaseLikeCount() }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("주문 가능 여부를 판단할 때,")
    @Nested
    inner class IsOrderable {
        @DisplayName("ON_SALE 상태이고 재고가 충분하면, true 를 반환한다.")
        @Test
        fun returnsTrue_whenOnSaleAndStockEnough() {
            // arrange
            val target = product(stockQuantity = 5, status = ProductStatus.ON_SALE)

            // assert
            assertThat(target.isOrderable(3)).isTrue()
        }

        @DisplayName("HIDDEN 상태면, false 를 반환한다.")
        @Test
        fun returnsFalse_whenHidden() {
            // arrange
            val target = product(stockQuantity = 5, status = ProductStatus.HIDDEN)

            // assert
            assertThat(target.isOrderable(3)).isFalse()
        }

        @DisplayName("재고가 부족하면, false 를 반환한다.")
        @Test
        fun returnsFalse_whenStockInsufficient() {
            // arrange
            val target = product(stockQuantity = 2, status = ProductStatus.ON_SALE)

            // assert
            assertThat(target.isOrderable(3)).isFalse()
        }

        @DisplayName("soft delete 상태면, false 를 반환한다.")
        @Test
        fun returnsFalse_whenSoftDeleted() {
            // arrange
            val target = product(stockQuantity = 5, status = ProductStatus.ON_SALE).also { it.delete() }

            // assert
            assertThat(target.isOrderable(3)).isFalse()
        }
    }

    @DisplayName("상품 정보를 수정할 때,")
    @Nested
    inner class Modify {
        @DisplayName("rename: 새 이름이 유효하면, 이름이 변경된다.")
        @Test
        fun rename_changesName_whenNewNameIsValid() {
            val target = product(name = "Air Max")
            target.rename("Air Force 1")
            assertThat(target.name).isEqualTo("Air Force 1")
        }

        @DisplayName("changePrice: 새 가격이 0 이상이면, 가격이 변경된다.")
        @Test
        fun changePrice_changesPrice_whenNewPriceIsValid() {
            val target = product(price = 100_000)
            target.changePrice(120_000)
            assertThat(target.price).isEqualTo(120_000L)
        }

        @DisplayName("changeStatus: 새 상태로 변경된다.")
        @Test
        fun changeStatus_changesStatus() {
            val target = product(status = ProductStatus.ON_SALE)
            target.changeStatus(ProductStatus.HIDDEN)
            assertThat(target.status).isEqualTo(ProductStatus.HIDDEN)
        }
    }
}
