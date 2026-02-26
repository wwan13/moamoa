package server.security

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.web.server.WebFilterChain
import reactor.core.publisher.Mono
import server.shared.security.jwt.AuthPrincipal
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException
import server.shared.security.jwt.TokenProvider
import server.shared.security.jwt.TokenType
import test.UnitTest

class TokenDecodeCacheFilterTest : UnitTest() {
    @Test
    fun `Bearer 헤더가 없으면 decodeToken을 호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        filter.filter(exchange, passThroughChain()).block()

        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
        exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] shouldBe null
        exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] shouldBe null
    }

    @Test
    fun `유효한 Bearer 토큰이면 principal을 캐시한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val principal = AuthPrincipal(memberId = 10L, type = TokenType.ACCESS, role = "USER")
        every { tokenProvider.decodeToken("access-token") } returns principal
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer access-token")
                .build()
        )

        filter.filter(exchange, passThroughChain()).block()

        verify(exactly = 1) { tokenProvider.decodeToken("access-token") }
        exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] shouldBe principal
        exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] shouldBe null
    }

    @Test
    fun `유효하지 않은 Bearer 토큰이면 예외를 캐시하고 요청은 계속 진행한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("invalid-token") } throws InvalidTokenException()
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
                .build()
        )
        var chainCalled = false
        val chain = WebFilterChain {
            chainCalled = true
            Mono.empty()
        }

        filter.filter(exchange, chain).block()

        verify(exactly = 1) { tokenProvider.decodeToken("invalid-token") }
        chainCalled shouldBe true
        (exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] is InvalidTokenException) shouldBe true
        exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] shouldBe null
    }

    @Test
    fun `만료된 Bearer 토큰이면 예외를 캐시한다`() {
        val tokenProvider = mockk<TokenProvider>()
        every { tokenProvider.decodeToken("expired-token") } throws ExpiredTokenException()
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
                .build()
        )

        filter.filter(exchange, passThroughChain()).block()

        verify(exactly = 1) { tokenProvider.decodeToken("expired-token") }
        (exchange.attributes[TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR] is ExpiredTokenException) shouldBe true
        exchange.attributes[TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR] shouldBe null
    }

    @Test
    fun `Bearer 형식이 아니면 decodeToken을 호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val filter = TokenDecodeCacheFilter(tokenProvider)
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/")
                .header(HttpHeaders.AUTHORIZATION, "Basic token")
                .build()
        )

        filter.filter(exchange, passThroughChain()).block()

        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
    }

    private fun passThroughChain(): WebFilterChain = WebFilterChain { Mono.empty() }
}
