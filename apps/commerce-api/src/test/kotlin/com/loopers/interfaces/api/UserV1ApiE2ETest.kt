package com.loopers.interfaces.api

import com.loopers.interfaces.api.user.UserV1Dto
import com.loopers.utils.DatabaseCleanUp
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertAll
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.boot.test.web.client.TestRestTemplate
import org.springframework.core.ParameterizedTypeReference
import org.springframework.http.HttpEntity
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import java.time.LocalDate

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class UserV1ApiE2ETest @Autowired constructor(
    private val testRestTemplate: TestRestTemplate,
    private val databaseCleanUp: DatabaseCleanUp,
) {
    companion object {
        private const val ENDPOINT_SIGN_UP = "/api/v1/users"
    }

    @AfterEach
    fun tearDown() {
        databaseCleanUp.truncateAllTables()
    }

    @DisplayName("POST /api/v1/users")
    @Nested
    inner class SignUp {
        @DisplayName("유효한 정보로 요청하면, 가입된 유저 정보를 반환한다.")
        @Test
        fun returnsUserResponse_whenValidDataIsProvided() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "user123",
                password = "Valid1!pw",
                name = "홍길동",
                birthDate = LocalDate.of(1994, 7, 14),
                email = "hong@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_SIGN_UP, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode.is2xxSuccessful).isTrue() },
                { assertThat(response.body?.data?.loginId).isEqualTo(request.loginId) },
                { assertThat(response.body?.data?.name).isEqualTo(request.name) },
                { assertThat(response.body?.data?.email).isEqualTo(request.email) },
            )
        }

        @DisplayName("이미 가입된 로그인 ID로 요청하면, 409 CONFLICT 응답을 받는다.")
        @Test
        fun returnsConflict_whenLoginIdAlreadyExists() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "user123",
                password = "Valid1!pw",
                name = "홍길동",
                birthDate = LocalDate.of(1994, 7, 14),
                email = "hong@example.com",
            )
            testRestTemplate.exchange(ENDPOINT_SIGN_UP, HttpMethod.POST, HttpEntity(request), object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {})

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_SIGN_UP, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.CONFLICT) },
            )
        }

        @DisplayName("로그인 ID에 특수문자가 포함되면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenLoginIdContainsSpecialCharacters() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "user@123",
                password = "Valid1!pw",
                name = "홍길동",
                birthDate = LocalDate.of(1994, 7, 14),
                email = "hong@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_SIGN_UP, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }

        @DisplayName("비밀번호 형식이 올바르지 않으면, 400 BAD_REQUEST 응답을 받는다.")
        @Test
        fun returnsBadRequest_whenPasswordIsInvalid() {
            // arrange
            val request = UserV1Dto.SignUpRequest(
                loginId = "user123",
                password = "short",
                name = "홍길동",
                birthDate = LocalDate.of(1994, 7, 14),
                email = "hong@example.com",
            )

            // act
            val responseType = object : ParameterizedTypeReference<ApiResponse<UserV1Dto.UserResponse>>() {}
            val response = testRestTemplate.exchange(ENDPOINT_SIGN_UP, HttpMethod.POST, HttpEntity(request), responseType)

            // assert
            assertAll(
                { assertThat(response.statusCode).isEqualTo(HttpStatus.BAD_REQUEST) },
            )
        }
    }
}
