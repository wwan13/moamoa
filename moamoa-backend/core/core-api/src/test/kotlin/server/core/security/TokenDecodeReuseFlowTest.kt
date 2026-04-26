package server.core.security

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.context.request.ServletWebRequest
import server.core.global.security.TokenDecodeCacheFilter
import server.core.feature.member.domain.MemberRole
import server.core.global.security.Passport
import server.core.global.security.PassportResolver
import server.core.global.security.RequestPassport
import server.token.AuthPrincipal
import server.token.TokenProvider
import server.token.TokenType
import test.UnitTest

class TokenDecodeReuseFlowTest : UnitTest() {
    @Test
    fun `같은 요청에서 필터 이후 리졸버가 decodeToken을 재호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("access-token") } returns AuthPrincipal(
            memberId = 42L,
            type = TokenType.ACCESS,
            role = "USER"
        )
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val resolver = PassportResolver(tokenProvider)

        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        val resolvedPassport = resolver.resolveArgument(
            methodParameter(),
            null,
            ServletWebRequest(request, response),
            null
        ) as Passport

        resolvedPassport shouldBe Passport(memberId = 42L, role = MemberRole.USER)
        verify(exactly = 1) { tokenProvider.decodeToken("access-token") }
    }

    private fun methodParameter(): MethodParameter =
        MethodParameter(TestHandler::class.java.getDeclaredMethod("required", Passport::class.java), 0).apply {
            initParameterNameDiscovery(DefaultParameterNameDiscoverer())
        }

    private class TestHandler {
        fun required(@RequestPassport passport: Passport) {
            // test target
        }
    }
}
