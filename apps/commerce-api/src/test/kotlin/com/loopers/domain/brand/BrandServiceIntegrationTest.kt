package com.loopers.domain.brand

import com.loopers.infrastructure.brand.BrandJpaRepository
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
class BrandServiceIntegrationTest @Autowired constructor(
    private val brandService: BrandService,
    private val brandJpaRepository: BrandJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("브랜드를 등록할 때,")
    @Nested
    inner class Register {
        @DisplayName("새로운 이름이면, 신규 브랜드가 저장된다.")
        @Test
        fun savesNewBrand_whenNameIsNew() {
            // arrange
            val name = "Nike"

            // act
            val result = brandService.register(name)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.deletedAt).isNull() },
            )
        }

        @DisplayName("이미 활성 상태로 동일한 이름이 존재하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenActiveBrandWithSameNameExists() {
            // arrange
            val name = "Nike"
            brandService.register(name)

            // act
            val result = assertThrows<CoreException> {
                brandService.register(name)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("동일한 이름의 soft-deleted 브랜드가 존재하면, 기존 행이 restore 된다.")
        @Test
        fun restoresExistingRow_whenSoftDeletedBrandWithSameNameExists() {
            // arrange
            val name = "Nike"
            val original = brandService.register(name)
            brandService.delete(original.id)

            // act
            val restored = brandService.register(name)

            // assert
            assertAll(
                { assertThat(restored.id).isEqualTo(original.id) },
                { assertThat(restored.name).isEqualTo(name) },
                { assertThat(restored.deletedAt).isNull() },
            )
        }
    }

    @DisplayName("브랜드를 ID로 조회할 때,")
    @Nested
    inner class GetById {
        @DisplayName("활성 브랜드가 존재하면, 해당 브랜드를 반환한다.")
        @Test
        fun returnsBrand_whenActiveBrandExists() {
            // arrange
            val saved = brandService.register("Nike")

            // act
            val result = brandService.getById(saved.id)

            // assert
            assertThat(result.id).isEqualTo(saved.id)
        }

        @DisplayName("존재하지 않는 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandDoesNotExist() {
            // arrange
            val nonExistentId = 999L

            // act
            val result = assertThrows<CoreException> {
                brandService.getById(nonExistentId)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("soft-deleted 브랜드를 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandIsSoftDeleted() {
            // arrange
            val saved = brandService.register("Nike")
            brandService.delete(saved.id)

            // act
            val result = assertThrows<CoreException> {
                brandService.getById(saved.id)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드 목록을 조회할 때,")
    @Nested
    inner class GetAll {
        @DisplayName("활성 브랜드만 페이징되어 반환된다.")
        @Test
        fun returnsActiveBrands_whenSoftDeletedBrandsExist() {
            // arrange
            val active = brandService.register("Nike")
            val toDelete = brandService.register("Adidas")
            brandService.delete(toDelete.id)

            // act
            val result = brandService.getAll(PageRequest.of(0, 10))

            // assert
            assertAll(
                { assertThat(result.totalElements).isEqualTo(1) },
                { assertThat(result.content.map { it.id }).containsExactly(active.id) },
            )
        }
    }

    @DisplayName("브랜드 이름을 변경할 때,")
    @Nested
    inner class Rename {
        @DisplayName("유효한 새 이름을 주면, 이름이 변경된다.")
        @Test
        fun changesName_whenNewNameIsValid() {
            // arrange
            val saved = brandService.register("Nike")

            // act
            brandService.rename(saved.id, "Adidas")

            // assert
            val reloaded = brandJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.name).isEqualTo("Adidas")
        }

        @DisplayName("존재하지 않는 ID로 변경하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenBrandDoesNotExist() {
            // act
            val result = assertThrows<CoreException> {
                brandService.rename(999L, "Adidas")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }
    }

    @DisplayName("브랜드를 삭제할 때,")
    @Nested
    inner class Delete {
        @DisplayName("활성 브랜드를 삭제하면, soft delete 처리된다.")
        @Test
        fun softDeletesBrand_whenBrandIsActive() {
            // arrange
            val saved = brandService.register("Nike")

            // act
            brandService.delete(saved.id)

            // assert
            val reloaded = brandJpaRepository.findById(saved.id).orElseThrow()
            assertThat(reloaded.deletedAt).isNotNull()
        }

        @DisplayName("이미 삭제된 브랜드를 다시 삭제해도, 예외 없이 처리된다 (멱등).")
        @Test
        fun isIdempotent_whenBrandIsAlreadyDeleted() {
            // arrange
            val saved = brandService.register("Nike")
            brandService.delete(saved.id)

            // act + assert
            brandService.delete(saved.id)
        }
    }
}
