package server.security

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.DefaultParameterNameDiscoverer
import org.springframework.core.MethodParameter
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.reactive.BindingContext
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import server.feature.member.command.domain.MemberRole
import server.shared.security.jwt.AuthPrincipal
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
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
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build()
        )
        var resolvedPassport: Passport? = null
        val chain = WebFilterChain {
            resolver.resolveArgument(
                methodParameter(),
                mockk<BindingContext>(),
                exchange
            )
                .doOnNext { resolvedPassport = it as Passport }
                .then(Mono.empty())
        }

        filter.filter(exchange, chain).block()

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
