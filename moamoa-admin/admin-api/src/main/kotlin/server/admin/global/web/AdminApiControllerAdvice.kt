package server.admin.global.web

import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import jakarta.servlet.http.HttpServletRequest
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.web.HttpMediaTypeNotAcceptableException
import org.springframework.web.HttpMediaTypeNotSupportedException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.bind.support.WebExchangeBindException
import server.admin.security.AdminForbiddenException
import server.admin.security.AdminUnauthorizedException
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException

@RestControllerAdvice
internal class AdminApiControllerAdvice {

    private val log = kLogger {}

    data class ErrorResponse(
        val status: Int,
        val message: String,
    )

    @ExceptionHandler(WebExchangeBindException::class, MethodArgumentNotValidException::class)
    fun handleValidation(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 값이 올바르지 않습니다", request.requestURI)
        return badRequest("요청 값이 올바르지 않습니다")
    }

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 본문(JSON)이 올바르지 않습니다", request.requestURI)
        return badRequest("요청 본문(JSON)이 올바르지 않습니다")
    }

    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingValue(request: HttpServletRequest, e: MissingServletRequestParameterException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 필수 값이 누락되었습니다: {}", request.requestURI, e.message)
        return badRequest("필수 값이 누락되었습니다")
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotAllowed(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 HTTP 메서드입니다", request.requestURI)
        return error(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다")
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException::class)
    fun handleUnsupportedMediaType(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 Content-Type 입니다", request.requestURI)
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다")
    }

    @ExceptionHandler(HttpMediaTypeNotAcceptableException::class)
    fun handleNotAcceptable(request: HttpServletRequest): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 Accept 입니다", request.requestURI)
        return error(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다")
    }

    @ExceptionHandler(AdminUnauthorizedException::class)
    fun handleUnauthorized(request: HttpServletRequest, e: AdminUnauthorizedException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    @ExceptionHandler(AdminForbiddenException::class)
    fun handleForbidden(request: HttpServletRequest, e: AdminForbiddenException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message)
        return error(HttpStatus.FORBIDDEN, e.message ?: "접근 권한이 없습니다")
    }

    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(request: HttpServletRequest, e: InvalidTokenException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    @ExceptionHandler(ExpiredTokenException::class)
    fun handleExpiredToken(request: HttpServletRequest, e: ExpiredTokenException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "TOKEN_EXPIRED")
    }

    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(request: HttpServletRequest, e: IllegalArgumentException): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message)
        return badRequest(e.message ?: "잘못된 요청입니다")
    }

    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(request: HttpServletRequest, e: IllegalStateException): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", request.requestURI, e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    @ExceptionHandler(Exception::class)
    fun handleException(request: HttpServletRequest, e: Exception): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", request.requestURI, e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> = error(HttpStatus.BAD_REQUEST, message)

    private fun error(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(ErrorResponse(status = status.value(), message = message))
}
