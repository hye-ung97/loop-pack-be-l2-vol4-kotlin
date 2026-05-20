package com.loopers.application.user

import com.loopers.domain.user.UserModel
import java.time.LocalDate

data class UserInfo(
    val id: Long,
    val loginId: String,
    val name: String,
    val birthDate: LocalDate,
    val email: String,
) {
    companion object {
        fun from(model: UserModel): UserInfo {
            return UserInfo(
                id = model.id,
                loginId = model.loginId,
                name = model.name,
                birthDate = model.birthDate,
                email = model.email,
            )
        }
    }
}
