package server.security

import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import server.feature.member.command.domain.MemberRole
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
        bindingContext: BindingContext,
        exchange: ServerWebExchange
    ): Mono<Any?> {
        val isNullable = parameter
            .method
            ?.kotlinFunction
            ?.parameters
            ?.firstOrNull { it.name == parameter.parameterName }
            ?.type
            ?.isMarkedNullable == true

        fun unauthorized(): Mono<Any?> =
            if (isNullable) Mono.justOrEmpty(null)
            else Mono.error(UnauthorizedException())

        val bearerToken = exchange.request.headers
            .getFirst(HttpHeaders.AUTHORIZATION)
            ?: return unauthorized()
        if (!bearerToken.startsWith("Bearer ", ignoreCase = true)) {
            return unauthorized()
        }
        val accessToken = bearerToken.removePrefix("Bearer").trim()

        val principal = tokenProvider.decodeToken(accessToken)
        if (principal.type != TokenType.ACCESS) {
            return unauthorized()
        }
        val role = principal.role ?: return unauthorized()

        return Mono.just(
            Passport(
                memberId = principal.memberId,
                role = MemberRole.valueOf(role)
            )
        )
    }
}