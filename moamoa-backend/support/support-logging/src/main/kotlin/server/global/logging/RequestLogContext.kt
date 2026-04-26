package server.global.logging

data class RequestLogContext(
    val traceId: String,
    val userId: Long?,
    val clientIp: String?,
)
