package server.security

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.ServletWebRequest
import server.feature.member.command.domain.MemberRole
import server.shared.security.jwt.AuthPrincipal
import server.shared.security.jwt.InvalidTokenException
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
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
        val webRequest = webRequest()

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(methodParameter("required"), null, webRequest, null)
        }
    }

    @Test
    fun `Authorization 헤더가 없고 nullable 이면 null을 반환한다`() {
        val resolver = PassportResolver(mockk())
        val webRequest = webRequest()

        val result = resolver.resolveArgument(methodParameter("optional"), null, webRequest, null)

        result shouldBe null
    }

    @Test
    fun `Bearer 토큰이 아니면 UnauthorizedException이 발생한다`() {
        val resolver = PassportResolver(mockk())
        val webRequest = webRequest(authorization = "Basic token")

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(methodParameter("required"), null, webRequest, null)
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
        val webRequest = webRequest(authorization = "Bearer refresh-token")

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(methodParameter("required"), null, webRequest, null)
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
        val webRequest = webRequest(authorization = "Bearer access-token")

        shouldThrow<UnauthorizedException> {
            resolver.resolveArgument(methodParameter("required"), null, webRequest, null)
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
        val webRequest = webRequest(authorization = "Bearer access-token")

        val result = resolver.resolveArgument(methodParameter("required"), null, webRequest, null)

        result shouldBe Passport(
            memberId = 42L,
            role = MemberRole.USER
        )
        verify(exactly = 1) { tokenProvider.decodeToken("access-token") }
    }

    @Test
    fun `캐시된 principal이 있으면 decodeToken을 호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val resolver = PassportResolver(tokenProvider)
        val webRequest = webRequest(authorization = "Bearer access-token")
        webRequest.request.setAttribute(
            TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR,
            AuthPrincipal(memberId = 77L, type = TokenType.ACCESS, role = "USER")
        )

        val result = resolver.resolveArgument(methodParameter("required"), null, webRequest, null)

        result shouldBe Passport(
            memberId = 77L,
            role = MemberRole.USER
        )
        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
    }

    @Test
    fun `캐시된 decode 예외가 있으면 그대로 전파한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val resolver = PassportResolver(tokenProvider)
        val webRequest = webRequest(authorization = "Bearer access-token")
        webRequest.request.setAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR, InvalidTokenException())

        shouldThrow<InvalidTokenException> {
            resolver.resolveArgument(methodParameter("required"), null, webRequest, null)
        }

        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
    }

    @Test
    fun `캐시가 없으면 decodeToken을 호출한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("access-token") } returns AuthPrincipal(
            memberId = 101L,
            type = TokenType.ACCESS,
            role = "USER"
        )
        val resolver = PassportResolver(tokenProvider)
        val webRequest = webRequest(authorization = "Bearer access-token")

        val result = resolver.resolveArgument(methodParameter("required"), null, webRequest, null)

        result shouldBe Passport(
            memberId = 101L,
            role = MemberRole.USER
        )
        verify(exactly = 1) { tokenProvider.decodeToken("access-token") }
    }

    private fun webRequest(authorization: String? = null): ServletWebRequest {
        val request = MockHttpServletRequest()
        request.method = "GET"
        if (authorization != null) {
            request.addHeader(HttpHeaders.AUTHORIZATION, authorization)
        }
        return ServletWebRequest(request, MockHttpServletResponse())
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
