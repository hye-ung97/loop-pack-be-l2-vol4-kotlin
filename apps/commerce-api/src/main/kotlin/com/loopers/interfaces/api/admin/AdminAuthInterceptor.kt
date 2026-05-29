package com.loopers.interfaces.api.admin

import com.loopers.domain.user.UserRepository
import com.loopers.domain.user.UserRole
import com.loopers.support.error.CoreException
import com.loopers.support.error.ErrorType
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.stereotype.Component
import org.springframework.web.servlet.HandlerInterceptor

@Component
class AdminAuthInterceptor(
    private val userRepository: UserRepository,
) : HandlerInterceptor {
    override fun preHandle(request: HttpServletRequest, response: HttpServletResponse, handler: Any): Boolean {
        val ldapId = request.getHeader(HEADER_LDAP)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "X-Loopers-Ldap 헤더가 누락되었습니다.")
        val user = userRepository.findByLoginId(ldapId)
            ?: throw CoreException(ErrorType.UNAUTHORIZED, "인증된 어드민이 아닙니다.")
        if (user.role != UserRole.ADMIN) {
            throw CoreException(ErrorType.UNAUTHORIZED, "어드민 권한이 없습니다.")
        }
        return true
    }

    companion object {
        const val HEADER_LDAP = "X-Loopers-Ldap"
    }
}
