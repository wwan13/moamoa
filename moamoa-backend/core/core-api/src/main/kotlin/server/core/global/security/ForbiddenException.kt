package server.core.global.security

class ForbiddenException(
    message: String = "접근 권한이 없습니다"
) : RuntimeException(message)
