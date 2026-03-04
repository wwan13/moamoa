package server.security

import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import server.feature.member.command.domain.MemberRole
import server.global.logging.RequestLogContextHolder
import server.shared.security.jwt.AuthPrincipal
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
import kotlin.reflect.jvm.kotlinFunction

@Component
class PassportResolver(
    private val tokenProvider: TokenProvider,
) : HandlerMethodArgumentResolver {

    override fun supportsParameter(parameter: MethodParameter): Boolean {
        val hasPassportAnnotation = parameter.hasParameterAnnotation(RequestPassport::class.java)
        val isPassportType = Passport::class.java.isAssignableFrom(parameter.parameterType)
        return hasPassportAnnotation && isPassportType
    }

    override fun resolveArgument(
        parameter: MethodParameter,
        mavContainer: ModelAndViewContainer?,
        webRequest: NativeWebRequest,
        binderFactory: WebDataBinderFactory?,
    ): Any? {
        val isNullable = parameter
            .method
            ?.kotlinFunction
            ?.parameters
            ?.firstOrNull { it.name == parameter.parameterName }
            ?.type
            ?.isMarkedNullable == true

        fun unauthorized(): Any? =
            if (isNullable) null
            else throw UnauthorizedException()

        val request = webRequest.getNativeRequest(HttpServletRequest::class.java) ?: return unauthorized()
        val bearerToken = request.getHeader(HttpHeaders.AUTHORIZATION) ?: return unauthorized()
        if (!bearerToken.startsWith("Bearer ", ignoreCase = true)) {
            return unauthorized()
        }
        val accessToken = bearerToken.removePrefix("Bearer").trim()

        val decodeError = request.getAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR) as? RuntimeException
        if (decodeError != null) {
            throw decodeError
        }

        val principal = (request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) as? AuthPrincipal)
            ?: tokenProvider.decodeToken(accessToken).also {
                request.setAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR, it)
            }
        if (principal.type != TokenType.ACCESS) {
            return unauthorized()
        }
        val role = principal.role ?: return unauthorized()

        request.setAttribute(RequestLogContextHolder.USER_ID_ATTR, principal.memberId.toString())

        return Passport(
            memberId = principal.memberId,
            role = MemberRole.valueOf(role)
        )
    }
}
