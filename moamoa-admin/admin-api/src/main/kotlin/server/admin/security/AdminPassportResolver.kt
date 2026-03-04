package server.admin.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import server.admin.feature.member.domain.AdminMemberRole
import server.global.logging.RequestLogContextHolder
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
import kotlin.reflect.jvm.kotlinFunction

@Component
class AdminPassportResolver(
    private val tokenProvider: TokenProvider,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasPassportAnnotation = parameter.hasParameterAnnotation(RequestAdminPassport::class.java)
        val isAdminPassportType = AdminPassport::class.java.isAssignableFrom(parameter.parameterType)
        return hasPassportAnnotation && isAdminPassportType
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val isNullable = parameter.method
            ?.kotlinFunction
            ?.parameters
            ?.firstOrNull { it.name == parameter.parameterName }
            ?.type
            ?.isMarkedNullable == true

        fun unauthorized(): Any? = if (isNullable) null else throw AdminUnauthorizedException()

        val request = webRequest.getNativeRequest(HttpServletRequest::class.java) ?: return unauthorized()
        val bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return unauthorized()
        if (!bearerToken.startsWith("Bearer ", ignoreCase = true)) return unauthorized()

        val accessToken = bearerToken.removePrefix("Bearer").trim()
        val principal = tokenProvider.decodeToken(accessToken)
        if (principal.type != TokenType.ACCESS) return unauthorized()

        val role = principal.role?.let { AdminMemberRole.valueOf(it) } ?: return unauthorized()
        if (role != AdminMemberRole.ADMIN) return unauthorized()
        request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, principal.memberId.toString())

        return AdminPassport(memberId = principal.memberId, role = role)
    }
}
