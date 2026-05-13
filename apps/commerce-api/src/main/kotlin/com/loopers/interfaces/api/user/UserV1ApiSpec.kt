package com.loopers.interfaces.api.user

import com.loopers.interfaces.api.ApiResponse
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.web.bind.annotation.RequestBody

@Tag(name = "User V1", description = "회원 API")
interface UserV1ApiSpec {
    @Operation(summary = "회원가입")
    fun signUp(@RequestBody request: UserV1Dto.SignUpRequest): ApiResponse<UserV1Dto.UserResponse>
}
