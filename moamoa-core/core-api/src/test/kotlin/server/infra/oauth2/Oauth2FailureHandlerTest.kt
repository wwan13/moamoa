package server.infra.oauth2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.AuthenticationException
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import test.UnitTest
import java.net.URI

class Oauth2FailureHandlerTest : UnitTest() {
    @Test
    fun `인증 실패 시 registrationId 로 authorization 경로로 리다이렉트한다`() {
        val handler = Oauth2FailureHandler()
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/login/oauth2/callback/google?code=1234").build()
        )

        handler.onAuthenticationFailure(
            WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
            object : AuthenticationException("fail") {}
        ).block()

        exchange.response.statusCode shouldBe HttpStatus.FOUND
        exchange.response.headers.location shouldBe URI.create("/oauth2/authorization/google")
    }

    @Test
    fun `registrationId 가 없으면 IllegalStateException이 발생한다`() {
        val handler = Oauth2FailureHandler()
        val exchange = MockServerWebExchange.from(
            MockServerHttpRequest.get("/login/oauth2/callback/").build()
        )

        shouldThrow<IllegalStateException> {
            handler.onAuthenticationFailure(
                WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
                object : AuthenticationException("fail") {}
            )
        }
    }
}
