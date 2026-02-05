package server.admin.security

import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.stereotype.Component
import org.springframework.web.reactive.BindingContext
import org.springframework.web.reactive.result.method.HandlerMethodArgumentResolver
import org.springframework.web.server.ServerWebExchange
import reactor.core.publisher.Mono
import server.admin.feature.member.domain.AdminMemberRole
import server.jwt.TokenProvider
import server.jwt.TokenType
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
            else Mono.error(AdminUnauthorizedException())

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
        val role = principal.role?.let { AdminMemberRole.valueOf(it) }
            ?: return unauthorized()

        if (role != AdminMemberRole.ADMIN) {
            return unauthorized()
        }

        return Mono.just(
            AdminPassport(
                memberId = principal.memberId,
                role = role
            )
        )
    }
}