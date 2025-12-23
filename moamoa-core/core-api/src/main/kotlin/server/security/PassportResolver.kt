package server.security

import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import server.domain.member.MemberRole
import server.jwt.TokenProvider
import server.jwt.TokenType

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
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any> {
        val headers = exchange.request.headers
        val bearerToken = headers[HttpHeaders.AUTHORIZATION]?.firstOrNull()
            ?: return Mono.error(UnauthorizedException())

        if (!bearerToken.startsWith("Bearer ", ignoreCase = true)) {
            return Mono.error(UnauthorizedException())
        }
        val accessToken = bearerToken.removePrefix("Bearer").trim()

        val principal = tokenProvider.decodeToken(accessToken)
        if (principal.type != TokenType.ACCESS) {
            return Mono.error(UnauthorizedException())
        }

        val role = principal.role ?: return Mono.error(UnauthorizedException())
        val passport = Passport(principal.memberId, MemberRole.valueOf(role))

        return Mono.just(passport)
    }
}
