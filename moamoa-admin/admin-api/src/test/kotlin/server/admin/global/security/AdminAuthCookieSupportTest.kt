package server.admin.global.security

import io.kotest.matchers.shouldBe
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.springframework.http.HttpHeaders
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import server.admin.global.security.appendAdminAuthCookies
import server.admin.global.security.expireAdminAuthCookies
import server.admin.global.security.resolveAdminAccessToken
import server.admin.global.security.resolveAdminRefreshToken
import test.UnitTest

class AdminAuthCookieSupportTest : UnitTest() {
    @Test
    fun `인증 쿠키를 발급한다`() {
        val response = MockHttpServletResponse()

        response.appendAdminAuthCookies("access-token", "refresh-token")

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        cookies.size shouldBe 2
        cookies[0].contains("accessToken=access-token") shouldBe true
        cookies[0].contains("HttpOnly") shouldBe true
        cookies[0].contains("Secure") shouldBe true
        cookies[0].contains("SameSite=None") shouldBe true
        cookies[0].contains("Max-Age=604800") shouldBe true
        cookies[1].contains("refreshToken=refresh-token") shouldBe true
        cookies[1].contains("Max-Age=604800") shouldBe true
    }

    @Test
    fun `로그아웃 쿠키를 만료한다`() {
        val response = MockHttpServletResponse()

        response.expireAdminAuthCookies()

        val cookies = response.getHeaders(HttpHeaders.SET_COOKIE)
        cookies.size shouldBe 2
        cookies[0].contains("accessToken=") shouldBe true
        cookies[0].contains("Max-Age=0") shouldBe true
        cookies[1].contains("refreshToken=") shouldBe true
        cookies[1].contains("Max-Age=0") shouldBe true
    }

    @Test
    fun `accessToken은 Authorization 헤더를 우선 사용한다`() {
        val request = MockHttpServletRequest().apply {
            addHeader(HttpHeaders.AUTHORIZATION, "Bearer header-token")
            setCookies(Cookie("accessToken", "cookie-token"))
        }

        request.resolveAdminAccessToken() shouldBe "header-token"
    }

    @Test
    fun `refreshToken은 헤더가 없으면 쿠키를 사용한다`() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("refreshToken", "cookie-refresh-token"))
        }

        request.resolveAdminRefreshToken() shouldBe "cookie-refresh-token"
    }
}
