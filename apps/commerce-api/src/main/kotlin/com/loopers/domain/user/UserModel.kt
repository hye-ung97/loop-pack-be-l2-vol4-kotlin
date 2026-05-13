package com.loopers.domain.user

import com.loopers.domain.BaseEntity
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.persistence.Entity
import jakarta.persistence.Table
import java.time.LocalDate

@Entity
@Table(name = "users")
class UserModel(
    loginId: String,
    password: String,
    name: String,
    birthDate: LocalDate,
    email: String,
) : BaseEntity() {
    var loginId: String = loginId
        protected set

    var password: String = password
        protected set

    var name: String = name
        protected set

    var birthDate: LocalDate = birthDate
        protected set

    var email: String = email
        protected set

    init {
        if (!loginId.matches(LOGIN_ID_REGEX)) {
            throw CoreException(ErrorType.BAD_REQUEST, "로그인 ID는 영문과 숫자만 허용됩니다.")
        }
        if (name.isBlank()) {
            throw CoreException(ErrorType.BAD_REQUEST, "이름은 비어있을 수 없습니다.")
        }
        if (!email.matches(EMAIL_REGEX)) {
            throw CoreException(ErrorType.BAD_REQUEST, "이메일 형식이 올바르지 않습니다.")
        }
    }

    companion object {
        private val LOGIN_ID_REGEX = Regex("^[A-Za-z0-9]+$")
        private val EMAIL_REGEX = Regex("^[^@]+@[^@]+\\.[^@]+")
    }
}
