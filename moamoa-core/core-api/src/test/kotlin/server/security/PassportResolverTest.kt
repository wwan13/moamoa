package server.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.BindingContext
import server.feature.member.command.domain.MemberRole
import server.jwt.AuthPrincipal
import server.jwt.TokenProvider
import server.jwt.TokenType
import test.UnitTest

class PassportResolverTest : UnitTest() {
    @Test
    fun `RequestPassport 애노테이션과 Passport 타입이면 지원한다`() {
        val resolver = PassportResolver(mockk())

        resolver.supportsParameter(methodParameter("required")) shouldBe true
        resolver.supportsParameter(methodParameter("noAnnotation")) shouldBe false
        resolver.supportsParameter(methodParameter("wrongType")) shouldBe false
    }

    @Test
    fun `Authorization 헤더가 없고 non-nullable 이면 UnauthorizedException이 발생한다`() {
        val resolver = PassportResolver(mockk())
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(
                methodParameter("required"),
                mockk<BindingContext>(),
                exchange
            ).block()
        }
    }

    @Test
    fun `Authorization 헤더가 없고 nullable 이면 null을 반환한다`() {
        val resolver = PassportResolver(mockk())
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        val result = resolver.resolveArgument(
            methodParameter("optional"),
            mockk<BindingContext>(),
            exchange
        ).block()

        result shouldBe null
    }

    @Test
    fun `Bearer 토큰이 아니면 UnauthorizedException이 발생한다`() {
        val resolver = PassportResolver(mockk())
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Basic token")
                .build()
        )

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(
                methodParameter("required"),
                mockk<BindingContext>(),
                exchange
            ).block()
        }
    }

    @Test
    fun `ACCESS 토큰이 아니면 UnauthorizedException이 발생한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("refresh-token") } returns AuthPrincipal(
            memberId = 1L,
            type = TokenType.REFRESH
        )
        val resolver = PassportResolver(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer refresh-token")
                .build()
        )

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(
                methodParameter("required"),
                mockk<BindingContext>(),
                exchange
            ).block()
        }
    }

    @Test
    fun `role 이 없으면 UnauthorizedException이 발생한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("access-token") } returns AuthPrincipal(
            memberId = 1L,
            type = TokenType.ACCESS,
            role = null
        )
        val resolver = PassportResolver(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build()
        )

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(
                methodParameter("required"),
                mockk<BindingContext>(),
                exchange
            ).block()
        }
    }

    @Test
    fun `정상 토큰이면 Passport를 반환한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("access-token") } returns AuthPrincipal(
            memberId = 42L,
            type = TokenType.ACCESS,
            role = "USER"
        )
        val resolver = PassportResolver(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build()
        )

        val result = resolver.resolveArgument(
            methodParameter("required"),
            mockk<BindingContext>(),
            exchange
        ).block()

        result shouldBe Passport(
            memberId = 42L,
            role = MemberRole.USER
        )
    }

    private fun methodParameter(name: String): MethodParameter {
        val method = when (name) {
            "required" -> TestHandler::class.java.getDeclaredMethod(name, Passport::class.java)
            "optional" -> TestHandler::class.java.getDeclaredMethod(name, Passport::class.java)
            "noAnnotation" -> TestHandler::class.java.getDeclaredMethod(name, Passport::class.java)
            "wrongType" -> TestHandler::class.java.getDeclaredMethod(name, String::class.java)
            else -> throw IllegalArgumentException("unknown method: $name")
        }
        return MethodParameter(method, 0).apply {
            initParameterNameDiscovery(DefaultParameterNameDiscoverer())
        }
    }

    private class TestHandler {
        fun required(@RequestPassport passport: Passport) {
            // test target
        }

        fun optional(@RequestPassport passport: Passport?) {
            // test target
        }

        fun noAnnotation(passport: Passport) {
            // test target
        }

        fun wrongType(@RequestPassport id: String) {
            // test target
        }
    }
}
