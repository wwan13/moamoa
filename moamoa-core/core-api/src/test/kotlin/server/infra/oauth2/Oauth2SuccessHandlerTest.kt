package server.infra.oauth2

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.mock.http.server.reactive.MockServerHttpRequest
import org.springframework.mock.web.server.MockServerWebExchange
import org.springframework.security.core.Authentication
import org.springframework.security.web.server.WebFilterExchange
import org.springframework.web.server.WebFilterChain
import server.feature.auth.application.AuthService
import server.feature.auth.application.AuthTokens
import server.feature.member.command.domain.MemberRole
import server.feature.member.command.domain.Provider
import server.infra.cache.RefreshTokenCache
import test.UnitTest
import java.net.URI

class Oauth2SuccessHandlerTest : UnitTest() {
    @Test
    fun `Authenticated 이면 토큰 발급 후 성공 리다이렉트한다`() {
        val authService = mockk<AuthService>()
        val refreshTokenCache = mockk<RefreshTokenCache>()
        val environment = mockk<Environment>()
        val handler = Oauth2SuccessHandler(authService, refreshTokenCache, environment)
        val authentication = mockk<Authentication>()
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        every { environment.activeProfiles } returns emptyArray()
        every { authentication.principal } returns Oauth2SocialUser.Authenticated(
            memberId = 10L,
            role = MemberRole.USER,
            isNew = false
        )
        every { authService.issueTokens(10L, "USER") } returns AuthTokens(
            accessToken = "access",
            refreshToken = "refresh"
        )
        coEvery { refreshTokenCache.set(10L, "refresh", any()) } returns Unit

        handler.onAuthenticationSuccess(
            WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
            authentication
        ).block()

        exchange.response.statusCode shouldBe HttpStatus.FOUND
        exchange.response.headers.location shouldBe URI.create(
            "http://localhost:5173/oauth2?type=success&accessToken=access&refreshToken=refresh&isNew=false"
        )
        coVerify(exactly = 1) { refreshTokenCache.set(10L, "refresh", 604_800_000L) }
    }

    @Test
    fun `EmailRequired 이면 이메일 입력 리다이렉트한다`() {
        val environment = mockk<Environment>()
        val handler = Oauth2SuccessHandler(
            authService = mockk(),
            refreshTokenCache = mockk(),
            environment = environment
        )
        val authentication = mockk<Authentication>()
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        every { environment.activeProfiles } returns emptyArray()
        every { authentication.principal } returns Oauth2SocialUser.EmailRequired(
            provider = Provider.GOOGLE,
            providerKey = "google-123"
        )

        handler.onAuthenticationSuccess(
            WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
            authentication
        ).block()

        exchange.response.statusCode shouldBe HttpStatus.FOUND
        exchange.response.headers.location shouldBe URI.create(
            "http://localhost:5173/oauth2?emailRequired=emailRequired&provider=GOOGLE&providerKey=google-123"
        )
    }

    @Test
    fun `HasError 이면 에러 리다이렉트한다`() {
        val environment = mockk<Environment>()
        val handler = Oauth2SuccessHandler(
            authService = mockk(),
            refreshTokenCache = mockk(),
            environment = environment
        )
        val authentication = mockk<Authentication>()
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        every { environment.activeProfiles } returns emptyArray()
        every { authentication.principal } returns Oauth2SocialUser.HasError(
            message = "boom"
        )

        handler.onAuthenticationSuccess(
            WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
            authentication
        ).block()

        exchange.response.statusCode shouldBe HttpStatus.FOUND
        exchange.response.headers.location shouldBe URI.create(
            "http://localhost:5173/oauth2?type=hasError&errorMessage=boom"
        )
    }

    @Test
    fun `prod 프로필이면 moamoa 도메인으로 리다이렉트한다`() {
        val environment = mockk<Environment>()
        val handler = Oauth2SuccessHandler(
            authService = mockk(),
            refreshTokenCache = mockk(),
            environment = environment
        )
        val authentication = mockk<Authentication>()
        val exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/").build())

        every { environment.activeProfiles } returns arrayOf("prod")
        every { authentication.principal } returns Oauth2SocialUser.HasError(
            message = "boom"
        )

        handler.onAuthenticationSuccess(
            WebFilterExchange(exchange, mockk<WebFilterChain>(relaxed = true)),
            authentication
        ).block()

        exchange.response.headers.location shouldBe URI.create(
            "https://moamoa.dev/oauth2?type=hasError&errorMessage=boom"
        )
    }
}
