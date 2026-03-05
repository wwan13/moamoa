package server.core.infra.oauth2

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import org.springframework.http.HttpStatus
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.AuthenticationException
import server.core.infra.oauth2.Oauth2FailureHandler
import test.UnitTest

class Oauth2FailureHandlerTest : UnitTest() {
    @Test
    fun `인증 실패 시 registrationId 로 authorization 경로로 리다이렉트한다`() {
        val handler = Oauth2FailureHandler()
        val request = MockHttpServletRequest("GET", "/auth/oauth2/callback/google")
        request.queryString = "code=1234"
        val response = MockHttpServletResponse()

        handler.onAuthenticationFailure(
            request,
            response,
            object : AuthenticationException("fail") {}
        )

        response.status shouldBe HttpStatus.FOUND.value()
        response.redirectedUrl shouldBe "/oauth2/authorization/google"
    }

    @Test
    fun `registrationId 가 없으면 IllegalStateException이 발생한다`() {
        val handler = Oauth2FailureHandler()
        val request = MockHttpServletRequest("GET", "/auth/oauth2/callback/")

        shouldThrow<IllegalStateException> {
            handler.onAuthenticationFailure(
                request,
                MockHttpServletResponse(),
                object : AuthenticationException("fail") {}
            )
        }
    }
}
