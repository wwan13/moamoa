package server.admin.global.security

import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpHeaders
import org.springframework.http.ResponseCookie

object AdminAuthCookieSupport {
    const val ACCESS_TOKEN_COOKIE = "accessToken"
    const val REFRESH_TOKEN_COOKIE = "refreshToken"
    const val REFRESH_TOKEN_HEADER = "X-Refresh-Token"
    const val ACCESS_TOKEN_MAX_AGE_SECONDS = 3_600L
    const val REFRESH_TOKEN_MAX_AGE_SECONDS = 604_800L
}

fun HttpServletResponse.appendAdminAuthCookies(accessToken: String, refreshToken: String) {
    addHeader(
        HttpHeaders.SET_COOKIE,
        adminAuthCookie(
            name = AdminAuthCookieSupport.ACCESS_TOKEN_COOKIE,
            value = accessToken,
            maxAge = AdminAuthCookieSupport.ACCESS_TOKEN_MAX_AGE_SECONDS
        )
    )
    addHeader(
        HttpHeaders.SET_COOKIE,
        adminAuthCookie(
            name = AdminAuthCookieSupport.REFRESH_TOKEN_COOKIE,
            value = refreshToken,
            maxAge = AdminAuthCookieSupport.REFRESH_TOKEN_MAX_AGE_SECONDS
        )
    )
}

fun HttpServletResponse.expireAdminAuthCookies() {
    addHeader(
        HttpHeaders.SET_COOKIE,
        adminAuthCookie(name = AdminAuthCookieSupport.ACCESS_TOKEN_COOKIE, value = "", maxAge = 0)
    )
    addHeader(
        HttpHeaders.SET_COOKIE,
        adminAuthCookie(name = AdminAuthCookieSupport.REFRESH_TOKEN_COOKIE, value = "", maxAge = 0)
    )
}

fun HttpServletRequest.resolveAdminAccessToken(): String? {
    val authorization = getHeader(HttpHeaders.AUTHORIZATION)
    if (!authorization.isNullOrBlank() && authorization.startsWith("Bearer ", ignoreCase = true)) {
        val token = authorization.substringAfter(' ', "").trim()
        if (token.isNotBlank()) return token
    }
    return adminCookieValue(AdminAuthCookieSupport.ACCESS_TOKEN_COOKIE)
}

fun HttpServletRequest.resolveAdminRefreshToken(): String? {
    val headerToken = getHeader(AdminAuthCookieSupport.REFRESH_TOKEN_HEADER)?.trim()
    if (!headerToken.isNullOrBlank()) return headerToken
    return adminCookieValue(AdminAuthCookieSupport.REFRESH_TOKEN_COOKIE)
}

private fun HttpServletRequest.adminCookieValue(name: String): String? {
    val requestCookies = cookies ?: return null
    return requestCookies.firstOrNull { it.name == name }?.value?.takeIf { it.isNotBlank() }
}

private fun adminAuthCookie(name: String, value: String, maxAge: Long): String {
    return ResponseCookie.from(name, value)
        .httpOnly(true)
        .secure(true)
        .sameSite("None")
        .path("/")
        .maxAge(maxAge)
        .build()
        .toString()
}
