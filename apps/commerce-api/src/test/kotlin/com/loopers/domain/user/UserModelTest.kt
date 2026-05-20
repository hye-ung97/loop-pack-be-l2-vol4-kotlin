package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.junit.jupiter.api.assertThrows
import java.time.LocalDate

class UserModelTest {
    @DisplayName("유저 모델을 생성할 때,")
    @Nested
    inner class Create {
        @DisplayName("필수 정보가 모두 유효하면, 정상적으로 생성된다.")
        @Test
        fun createsUserModel_whenAllRequiredFieldsAreValid() {
            // arrange
            val loginId = "user123"
            val password = "encodedPassword"
            val name = "홍길동"
            val birthDate = LocalDate.of(1994, 7, 14)
            val email = "hong@example.com"

            // act
            val user = UserModel(
                loginId = loginId,
                password = password,
                name = name,
                birthDate = birthDate,
                email = email,
            )

            // assert
            assertAll(
                { assertThat(user.loginId).isEqualTo(loginId) },
                { assertThat(user.name).isEqualTo(name) },
                { assertThat(user.birthDate).isEqualTo(birthDate) },
                { assertThat(user.email).isEqualTo(email) },
            )
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val invalidLoginId = "user@123"

            // act
            val result = assertThrows<CoreException> {
                UserModel(loginId = invalidLoginId, password = "encodedPw", name = "홍길동", birthDate = LocalDate.of(1994, 7, 14), email = "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("로그인 ID가 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdIsBlank() {
            // arrange
            val blankLoginId = "   "

            // act
            val result = assertThrows<CoreException> {
                UserModel(loginId = blankLoginId, password = "encodedPw", name = "홍길동", birthDate = LocalDate.of(1994, 7, 14), email = "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이름이 비어있으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenNameIsBlank() {
            // arrange
            val blankName = ""

            // act
            val result = assertThrows<CoreException> {
                UserModel(loginId = "user123", password = "encodedPw", name = blankName, birthDate = LocalDate.of(1994, 7, 14), email = "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("이메일 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenEmailFormatIsInvalid() {
            // arrange
            val invalidEmail = "not-an-email"

            // act
            val result = assertThrows<CoreException> {
                UserModel(loginId = "user123", password = "encodedPw", name = "홍길동", birthDate = LocalDate.of(1994, 7, 14), email = invalidEmail)
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
