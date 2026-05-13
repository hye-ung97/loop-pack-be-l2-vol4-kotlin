package com.loopers.interfaces.api.user

import com.loopers.application.user.UserInfo
import java.time.LocalDate

class UserV1Dto {
    data class SignUpRequest(
        val loginId: String,
        val password: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    )

    data class UserResponse(
        val id: Long,
        val loginId: String,
        val name: String,
        val birthDate: LocalDate,
        val email: String,
    ) {
        companion object {
            fun from(info: UserInfo): UserResponse {
                return UserResponse(
                    id = info.id,
                    loginId = info.loginId,
                    name = info.name,
                    birthDate = info.birthDate,
                    email = info.email,
                )
            }
        }
    }
}
