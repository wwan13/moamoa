package server.global.web

import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.servlet.http.HttpServletRequest
import org.springframework.core.env.Environment
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import server.WebhookSender
import server.content.WebhookContent
import server.global.logging.RequestLogContextHolder
import server.global.logging.errorType
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException
import server.security.ForbiddenException
import server.security.UnauthorizedException
import support.profile.isProd

@RestControllerAdvice
class ApiControllerAdvice(
    private val environment: Environment,
    private val webhookSender: WebhookSender,
) {

    private val logger = KotlinLogging.logger {}

    data class ErrorResponse(
        val status: Int,
        val message: String
    )

    @ExceptionHandler(BindException::class)
    fun handleBindException(
        request: HttpServletRequest,
        e: BindException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 400, "요청 값이 올바르지 않습니다", e)
        return badRequest("요청 값이 올바르지 않습니다")
    }

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(
        request: HttpServletRequest,
        e: MethodArgumentNotValidException,
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 400, "요청 값이 올바르지 않습니다", e)
        return badRequest("요청 값이 올바르지 않습니다")
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        request: HttpServletRequest,
        e: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 400, "요청 본문(JSON)이 올바르지 않습니다", e)
        return badRequest("요청 본문(JSON)이 올바르지 않습니다")
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingValue(
        request: HttpServletRequest,
        e: MissingServletRequestParameterException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 400, "필수 값이 누락되었습니다 reason=${e.message}", e)
        return badRequest("필수 값이 누락되었습니다")
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(
        request: HttpServletRequest,
        e: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 405, "지원하지 않는 HTTP 메서드입니다", e)
        return error(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다")
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(
        request: HttpServletRequest,
        e: HttpMediaTypeNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 415, "지원하지 않는 Content-Type 입니다", e)
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다")
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleNotAcceptable(
        request: HttpServletRequest,
        e: HttpMediaTypeNotAcceptableException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 406, "지원하지 않는 Accept 입니다", e)
        return error(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다")
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        request: HttpServletRequest,
        e: IllegalArgumentException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 400, e.message ?: "잘못된 요청입니다", e)
        return badRequest(e.message ?: "잘못된 요청입니다")
    }

    @ExceptionHandler(UnauthorizedException::class)
    fun handleUnauthorized(
        request: HttpServletRequest,
        e: UnauthorizedException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 401, e.message ?: "LOGIN_AGAIN", e)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    @ExceptionHandler(ForbiddenException::class)
    fun handleForbidden(
        request: HttpServletRequest,
        e: ForbiddenException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 403, e.message ?: "접근 권한이 없습니다", e)
        return error(HttpStatus.FORBIDDEN, e.message ?: "접근 권한이 없습니다")
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        request: HttpServletRequest,
        e: InvalidTokenException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 401, e.message ?: "LOGIN_AGAIN", e)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    @ExceptionHandler(ExpiredTokenException::class)
    fun handleExpiredToken(
        request: HttpServletRequest,
        e: ExpiredTokenException
    ): ResponseEntity<ErrorResponse> {
        logClientError(request, 401, e.message ?: "TOKEN_EXPIRED", e)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "TOKEN_EXPIRED")
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        request: HttpServletRequest,
        e: IllegalStateException
    ): ResponseEntity<ErrorResponse> {
        sendWebhook(request, e)
        logServerError(request, e.message ?: "서버 오류가 발생했습니다", e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    @ExceptionHandler(Exception::class)
    fun handleException(
        request: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        sendWebhook(request, e)
        logServerError(request, e.message ?: "서버 오류가 발생했습니다", e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, message)

    private fun error(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), message = message)
        )

    private fun sendWebhook(
        request: HttpServletRequest,
        e: Exception
    ) {
        if (!environment.isProd()) return

        val content = WebhookContent.Error(
            title = "서버 오류",
            description = "알 수 없는 오류가 발생했습니다.",
            fields = listOf(
                "apiPath" to request.requestURI,
                "errorMessage" to e.message.orEmpty(),
                "stackTrace" to e.stackTraceToString(),
            )
        )

        webhookSender.sendAsync(content)
    }

    private fun traceId(request: HttpServletRequest): String? =
        request.getAttribute(RequestLogContextHolder.TRACE_ID_ATTR)?.toString()
            ?: RequestLogContextHolder.current()?.traceId

    private fun logClientError(
        request: HttpServletRequest,
        status: Int,
        reason: String,
        throwable: Throwable
    ) {
        logger.errorType.warn(
            traceId = traceId(request),
            throwable = throwable,
            "call" to "api.request",
            "errorType" to "ClientError",
            "message" to reason,
            "path" to request.requestURI,
            "status" to status,
        ) { "요청 처리 중 클라이언트 오류가 발생했습니다" }
    }

    private fun logServerError(
        request: HttpServletRequest,
        reason: String,
        throwable: Throwable
    ) {
        logger.errorType.error(
            traceId = traceId(request),
            throwable = throwable,
            "call" to "api.request",
            "errorType" to (throwable::class.simpleName ?: "UnknownException"),
            "message" to reason,
            "path" to request.requestURI,
            "status" to 500,
        ) { "요청 처리 중 서버 오류가 발생했습니다" }
    }
}
