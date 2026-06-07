package com.loopers.domain.like

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll

class LikeModelTest {
    @DisplayName("좋아요 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("userId 와 productId 를 주면, 정상적으로 생성된다.")
        @Test
        fun createsLikeModel_whenValuesAreValid() {
            // act
            val like = LikeModel(userId = 1L, productId = 100L)

            // assert
            assertAll(
                { assertThat(like.userId).isEqualTo(1L) },
                { assertThat(like.productId).isEqualTo(100L) },
            )
        }
    }
}
