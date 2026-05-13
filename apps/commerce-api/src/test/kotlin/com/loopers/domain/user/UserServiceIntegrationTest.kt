package com.loopers.domain.user

import com.loopers.infrastructure.user.UserJpaRepository
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
import java.time.LocalDate

@SpringBootTest
class UserServiceIntegrationTest @Autowired constructor(
    private val userService: UserService,
    private val userJpaRepository: UserJpaRepository,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    private val birthDate = LocalDate.of(1994, 7, 14)

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("회원가입을 할 때,")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 정보를 주면, 저장된 유저 정보를 반환한다.")
        @Test
        fun returnsUserModel_whenValidDataIsProvided() {
            // arrange
            val loginId = "user123"
            val rawPassword = "Valid1!password"
            val name = "홍길동"
            val email = "hong@example.com"

            // act
            val result = userService.signUp(loginId, rawPassword, name, birthDate, email)

            // assert
            assertAll(
                { assertThat(result.id).isNotNull() },
                { assertThat(result.loginId).isEqualTo(loginId) },
                { assertThat(result.name).isEqualTo(name) },
                { assertThat(result.email).isEqualTo(email) },
            )
        }

        @DisplayName("이미 가입된 로그인 ID로 가입하면, CONFLICT 예외가 발생한다.")
        @Test
        fun throwsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val loginId = "user123"
            userService.signUp(loginId, "Valid1!password", "홍길동", birthDate, "hong@example.com")

            // act
            val result = assertThrows<CoreException> {
                userService.signUp(loginId, "Valid1!password2", "김철수", birthDate, "kim@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.CONFLICT)
        }

        @DisplayName("비밀번호가 8자 미만이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordIsTooShort() {
            // arrange
            val shortPassword = "Ab1!"

            // act
            val result = assertThrows<CoreException> {
                userService.signUp("user123", shortPassword, "홍길동", birthDate, "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호가 16자 초과이면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordIsTooLong() {
            // arrange
            val longPassword = "Ab1!Ab1!Ab1!Ab1!X"

            // act
            val result = assertThrows<CoreException> {
                userService.signUp("user123", longPassword, "홍길동", birthDate, "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 생년월일이 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsBirthDate() {
            // arrange - birthDate is 1994-07-14, formatted as "19940714"
            val passwordWithBirthDate = "19940714Ab!"

            // act
            val result = assertThrows<CoreException> {
                userService.signUp("user123", passwordWithBirthDate, "홍길동", birthDate, "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }

        @DisplayName("비밀번호에 허용되지 않는 문자(공백)가 포함되면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenPasswordContainsDisallowedCharacters() {
            // arrange
            val passwordWithSpace = "Ab1! cd2@"

            // act
            val result = assertThrows<CoreException> {
                userService.signUp("user123", passwordWithSpace, "홍길동", birthDate, "hong@example.com")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }

    @DisplayName("내 정보를 조회할 때,")
    @Nested
    inner class GetMyInfo {
        @DisplayName("유효한 인증 정보를 주면, 유저 정보를 반환한다.")
        @Test
        fun returnsUserModel_whenValidCredentialsAreProvided() {
            // arrange
            val loginId = "user123"
            val rawPassword = "Valid1!pw"
            userService.signUp(loginId, rawPassword, "홍길동", birthDate, "hong@example.com")

            // act
            val result = userService.authenticate(loginId, rawPassword)

            // assert
            assertAll(
                { assertThat(result.loginId).isEqualTo(loginId) },
                { assertThat(result.name).isEqualTo("홍길동") },
            )
        }

        @DisplayName("존재하지 않는 로그인 ID로 조회하면, NOT_FOUND 예외가 발생한다.")
        @Test
        fun throwsNotFound_whenLoginIdDoesNotExist() {
            // arrange
            val nonExistentLoginId = "ghost"

            // act
            val result = assertThrows<CoreException> {
                userService.authenticate(nonExistentLoginId, "anyPassword")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.NOT_FOUND)
        }

        @DisplayName("비밀번호가 틀리면, UNAUTHORIZED 예외가 발생한다.")
        @Test
        fun throwsUnauthorized_whenPasswordIsWrong() {
            // arrange
            userService.signUp("user123", "Valid1!pw", "홍길동", birthDate, "hong@example.com")

            // act
            val result = assertThrows<CoreException> {
                userService.authenticate("user123", "WrongPw1!")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.UNAUTHORIZED)
        }

        @DisplayName("로그인 ID 형식이 올바르지 않으면, BAD_REQUEST 예외가 발생한다.")
        @Test
        fun throwsBadRequest_whenLoginIdFormatIsInvalid() {
            // arrange
            val invalidLoginId = "user@123"

            // act
            val result = assertThrows<CoreException> {
                userService.authenticate(invalidLoginId, "Valid1!pw")
            }

            // assert
            assertThat(result.errorType).isEqualTo(ErrorType.BAD_REQUEST)
        }
    }
}
