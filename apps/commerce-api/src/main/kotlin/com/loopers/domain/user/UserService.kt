package com.loopers.domain.user

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate

@Component
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
) {
    @Transactional
    fun signUp(loginId: String, rawPassword: String, name: String, birthDate: LocalDate, email: String): UserModel {
        val user = UserModel(
            loginId = loginId,
            password = rawPassword,
            name = name,
            birthDate = birthDate,
            email = email,
        )
        return userRepository.save(user)
    }
}
