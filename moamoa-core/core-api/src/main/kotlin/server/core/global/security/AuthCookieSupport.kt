package server.core.global.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

object AuthCookieSupport {
    const val ACCESS_TOKEN_COOKIE = "accessToken"
    const val REFRESH_TOKEN_COOKIE = "refreshToken"
    const val REFRESH_TOKEN_HEADER = "X-Refresh-Token"
    const val ACCESS_TOKEN_MAX_AGE_SECONDS = 3_600L
    const val REFRESH_TOKEN_MAX_AGE_SECONDS = 604_800L

}

fun HttpServletResponse.appendAuthCookies(accessToken: String, refreshToken: String) {
    addHeader(
        HttpHeaders.SET_COOKIE,
        authCookie(
            name = AuthCookieSupport.ACCESS_TOKEN_COOKIE,
            value = accessToken,
            maxAge = AuthCookieSupport.ACCESS_TOKEN_MAX_AGE_SECONDS
        )
    )
    addHeader(
        HttpHeaders.SET_COOKIE,
        authCookie(
            name = AuthCookieSupport.REFRESH_TOKEN_COOKIE,
            value = refreshToken,
            maxAge = AuthCookieSupport.REFRESH_TOKEN_MAX_AGE_SECONDS
        )
    )
}

fun HttpServletResponse.expireAuthCookies() {
    addHeader(
        HttpHeaders.SET_COOKIE,
        authCookie(name = AuthCookieSupport.ACCESS_TOKEN_COOKIE, value = "", maxAge = 0)
    )
    addHeader(
        HttpHeaders.SET_COOKIE,
        authCookie(name = AuthCookieSupport.REFRESH_TOKEN_COOKIE, value = "", maxAge = 0)
    )
}

fun HttpServletRequest.resolveAccessToken(): String? {
    val authorization = getHeader(HttpHeaders.AUTHORIZATION)
    if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ", ignoreCase = true)) {
        val token = authorization.substringAfter(' ', "").trim()
        if (token.isNotBlank()) return token
    }
    return cookieValue(AuthCookieSupport.ACCESS_TOKEN_COOKIE)
}

fun HttpServletRequest.resolveRefreshToken(): String? {
    val headerToken = getHeader(AuthCookieSupport.REFRESH_TOKEN_HEADER)?.trim()
    if (!headerToken.isNullOrBlank()) return headerToken
    return cookieValue(AuthCookieSupport.REFRESH_TOKEN_COOKIE)
}

private fun HttpServletRequest.cookieValue(name: String): String? {
    val requestCookies = cookies ?: return null
    return requestCookies.firstOrNull { it.name == name }?.value?.takeIf { it.isNotBlank() }
}

private fun authCookie(name: String, value: String, maxAge: Long): String {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(true)
        .sameSite("None")
        .path("/")
        .maxAge(maxAge)
        .build()
        .toString()
}
