package com.loopers.application.user

import com.loopers.domain.user.UserService
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
class UserFacade(
    private val userService: UserService,
) {
    fun signUp(loginId: String, rawPassword: String, name: String, birthDate: LocalDate, email: String): UserInfo {
        return userService.signUp(loginId, rawPassword, name, birthDate, email)
            .let { UserInfo.from(it) }
    }

    fun getMyInfo(loginId: String, rawPassword: String): UserInfo {
        val info = userService.authenticate(loginId, rawPassword).let(UserInfo::from)
        return info.copy(name = info.name.dropLast(1) + "*")
    }

    fun changePassword(loginId: String, currentRawPassword: String, newRawPassword: String) {
        userService.changePassword(loginId, currentRawPassword, newRawPassword)
    }
}
