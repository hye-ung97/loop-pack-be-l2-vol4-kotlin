package com.loopers.domain.user

import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Component
@Transactional(readOnly = true)
class UserService(
    private val userRepository: UserRepository,
    private val passwordEncoder: BCryptPasswordEncoder,
) {
    @Transactional
    fun signUp(loginId: String, rawPassword: String, name: String, birthDate: LocalDate, email: String): UserModel {
        validatePassword(rawPassword, birthDate)
        if (userRepository.findByLoginId(loginId) != null) {
            throw CoreException(ErrorType.CONFLICT, "이미 사용 중인 로그인 ID입니다.")
        }
        val user = UserModel(
            loginId = loginId,
            password = passwordEncoder.encode(rawPassword),
            name = name,
            birthDate = birthDate,
            email = email,
        )
        return userRepository.save(user)
    }

    private fun validatePassword(rawPassword: String, birthDate: LocalDate) {
        if (rawPassword.length < 8 || rawPassword.length > 16) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 8~16자여야 합니다.")
        }
        if (!rawPassword.matches(Regex("^[A-Za-z0-9!@#\$%^&*()_+\\-=\\[\\]{};':\"\\\\|,.<>/?]+$"))) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호는 영문 대소문자, 숫자, 특수문자만 사용 가능합니다.")
        }
        val birthDateStr = birthDate.format(DateTimeFormatter.ofPattern("yyyyMMdd"))
        if (rawPassword.contains(birthDateStr)) {
            throw CoreException(ErrorType.BAD_REQUEST, "비밀번호에 생년월일을 포함할 수 없습니다.")
        }
    }
}
