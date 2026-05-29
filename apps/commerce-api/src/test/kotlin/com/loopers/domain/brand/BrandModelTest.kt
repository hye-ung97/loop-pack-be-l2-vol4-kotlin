package com.loopers.domain.brand

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class BrandModelTest {
    @DisplayName("브랜드 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("유효한 이름을 주면, 정상적으로 생성된다.")
        @Test
        fun createsBrandModel_whenNameIsValid() {
            // arrange
            val name = "Nike"

            // act
            val brand = BrandModel(name = name)

            // assert
            assertThat(brand.name).isEqualTo(name)
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // arrange
            val blankName = "   "

            // act
            val result = assertThrows<CoreException> {
                BrandModel(name = blankName)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("브랜드 이름을 변경할 때,")
    @Nested
    inner class Rename {
        @DisplayName("유효한 새 이름을 주면, 이름이 변경된다.")
        @Test
        fun changesName_whenNewNameIsValid() {
            // arrange
            val brand = BrandModel(name = "Nike")
            val newName = "Adidas"

            // act
            brand.rename(newName)

            // assert
            assertThat(brand.name).isEqualTo(newName)
        }

        @DisplayName("새 이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNewNameIsBlank() {
            // arrange
            val brand = BrandModel(name = "Nike")

            // act
            val result = assertThrows<CoreException> {
                brand.rename("   ")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
