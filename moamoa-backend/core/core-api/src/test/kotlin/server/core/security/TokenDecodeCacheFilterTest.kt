package server.core.security

import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockFilterChain
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import server.core.feature.member.infra.BlackListSet
import server.core.global.security.ForbiddenException
import server.core.global.security.TokenDecodeCacheAttributes
import server.core.global.security.TokenDecodeCacheFilter
import server.token.AuthPrincipal
import server.token.ExpiredTokenException
import server.token.InvalidTokenException
import server.token.TokenProvider
import server.token.TokenType
import test.UnitTest

class TokenDecodeCacheFilterTest : UnitTest() {
    @Test
    fun `Bearer 헤더가 없으면 decodeToken을 호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
        verify(exactly = 0) { blackListSet.contains(any()) }
        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe null
        request.getAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR) shouldBe null
    }

    @Test
    fun `유효한 Bearer 토큰이면 principal을 캐시한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val principal = AuthPrincipal(memberId = 10L, type = TokenType.ACCESS, role = "USER")
        every { tokenProvider.decodeToken("access-token") } returns principal
        every { blackListSet.contains(10L) } returns false
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer access-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 1) { tokenProvider.decodeToken("access-token") }
        verify(exactly = 1) { blackListSet.contains(10L) }
        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe principal
        request.getAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR) shouldBe null
    }

    @Test
    fun `유효하지 않은 Bearer 토큰이면 예외를 캐시하고 요청은 계속 진행한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        every { tokenProvider.decodeToken("invalid-token") } throws InvalidTokenException()
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer invalid-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 1) { tokenProvider.decodeToken("invalid-token") }
        verify(exactly = 0) { blackListSet.contains(any()) }
        (request.getAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR) is InvalidTokenException) shouldBe true
        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe null
    }

    @Test
    fun `만료된 Bearer 토큰이면 예외를 캐시한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        every { tokenProvider.decodeToken("expired-token") } throws ExpiredTokenException()
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer expired-token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 1) { tokenProvider.decodeToken("expired-token") }
        verify(exactly = 0) { blackListSet.contains(any()) }
        (request.getAttribute(TokenDecodeCacheAttributes.TOKEN_DECODE_ERROR_ATTR) is ExpiredTokenException) shouldBe true
        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe null
    }

    @Test
    fun `Bearer 형식이 아니면 decodeToken을 호출하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic token")
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 0) { tokenProvider.decodeToken(any()) }
        verify(exactly = 0) { blackListSet.contains(any()) }
    }

    @Test
    fun `Authorization 헤더가 없어도 accessToken 쿠키가 있으면 decodeToken을 호출한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val principal = AuthPrincipal(memberId = 11L, type = TokenType.ACCESS, role = "USER")
        every { tokenProvider.decodeToken("cookie-token") } returns principal
        every { blackListSet.contains(11L) } returns false
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/").apply {
            setCookies(Cookie("accessToken", "cookie-token"))
        }
        val response = MockHttpServletResponse()

        filter.doFilter(request, response, MockFilterChain())

        verify(exactly = 1) { tokenProvider.decodeToken("cookie-token") }
        verify(exactly = 1) { blackListSet.contains(11L) }
        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe principal
    }

    @Test
    fun `블랙리스트 사용자면 ForbiddenException을 캐시하고 principal은 저장하지 않는다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val principal = AuthPrincipal(memberId = 19L, type = TokenType.ACCESS, role = "USER")
        every { tokenProvider.decodeToken("blocked-token") } returns principal
        every { blackListSet.contains(19L) } returns true
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer blocked-token")

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe null
        (request.getAttribute(TokenDecodeCacheAttributes.BLACKLIST_ERROR_ATTR) as ForbiddenException).message shouldBe "차단된 사용자입니다"
    }

    @Test
    fun `블랙리스트 조회가 실패하면 fail open 으로 principal을 저장한다`() {
        val tokenProvider = mockk<TokenProvider>()
        val blackListSet = mockk<BlackListSet>()
        val principal = AuthPrincipal(memberId = 19L, type = TokenType.ACCESS, role = "USER")
        every { tokenProvider.decodeToken("blocked-token") } returns principal
        every { blackListSet.contains(19L) } throws IllegalStateException("redis down")
        val filter = TokenDecodeCacheFilter(tokenProvider, blackListSet)
        val request = MockHttpServletRequest("GET", "/")
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer blocked-token")

        filter.doFilter(request, MockHttpServletResponse(), MockFilterChain())

        request.getAttribute(TokenDecodeCacheAttributes.AUTH_PRINCIPAL_ATTR) shouldBe principal
        request.getAttribute(TokenDecodeCacheAttributes.BLACKLIST_ERROR_ATTR) shouldBe null
    }
}
