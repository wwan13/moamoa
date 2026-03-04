package server.infra.oauth2

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.authentication.AuthenticationFailureHandler
import org.springframework.stereotype.Component

@Component
class Oauth2FailureHandler : AuthenticationFailureHandler {
    override fun onAuthenticationFailure(
        request: HttpServletRequest,
        response: HttpServletResponse,
        exception: AuthenticationException?,
    ) {
        val path = request.requestURI

        val registrationId = path
            .substringAfter("/auth/oauth2/callback/")
            .substringBefore("?")
            .takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("registrationId 를 path 에서 찾을 수 없습니다")

        response.status = HttpStatus.FOUND.value()
        response.sendRedirect("/oauth2/authorization/$registrationId")
    }
}
