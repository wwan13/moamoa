package server.admin.global.web

import io.github.oshai.kotlinlogging.KotlinLogging.logger as kLogger
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.server.*
import server.admin.security.AdminForbiddenException
import server.admin.security.AdminUnauthorizedException
import server.shared.security.jwt.ExpiredTokenException
import server.shared.security.jwt.InvalidTokenException

@RestControllerAdvice
internal class AdminApiControllerAdvice {

    private val log = kLogger {}

    data class ErrorResponse(
        val status: Int,
        val message: String
    )

    // 400 - 바인딩/검증 실패 (@ModelAttribute 유사)
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        exchange: ServerWebExchange,
        e: BindException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 값이 올바르지 않습니다", exchange.request.path.value())
        return badRequest("요청 값이 올바르지 않습니다")
    }


    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handle(
        exchange: ServerWebExchange,
        e: MethodArgumentNotValidException,
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 값이 올바르지 않습니다", exchange.request.path.value())
        return badRequest("요청 값이 올바르지 않습니다")
    }

    // 400 - JSON 파싱/바인딩 실패 (body 관련)
    @ExceptionHandler(ServerWebInputException::class)
    fun handleWebInput(
        exchange: ServerWebExchange,
        e: ServerWebInputException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 본문/값이 올바르지 않습니다", exchange.request.path.value())
        return badRequest("요청 본문/값이 올바르지 않습니다")
    }

    // 400 - JSON 파싱 실패(케이스에 따라 얘도 올 수 있음)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        exchange: ServerWebExchange,
        e: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 본문(JSON)이 올바르지 않습니다", exchange.request.path.value())
        return badRequest("요청 본문(JSON)이 올바르지 않습니다")
    }

    // 400 - 필수 값 누락(query/header/path 등)
    @ExceptionHandler(MissingRequestValueException::class)
    fun handleMissingValue(
        exchange: ServerWebExchange,
        e: MissingRequestValueException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 필수 값이 누락되었습니다: {}", exchange.request.path.value(), e.reason)
        return badRequest("필수 값이 누락되었습니다")
    }

    // 405 - 메서드 미지원
    @ExceptionHandler(MethodNotAllowedException::class)
    fun handleMethodNotAllowed(
        exchange: ServerWebExchange,
        e: MethodNotAllowedException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 HTTP 메서드입니다", exchange.request.path.value())
        return error(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다")
    }

    // 415 - Content-Type 미지원
    @ExceptionHandler(UnsupportedMediaTypeStatusException::class)
    fun handleUnsupportedMediaType(
        exchange: ServerWebExchange,
        e: UnsupportedMediaTypeStatusException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 Content-Type 입니다", exchange.request.path.value())
        return error(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "지원하지 않는 Content-Type 입니다")
    }

    // 406 - Accept 미지원
    @ExceptionHandler(NotAcceptableStatusException::class)
    fun handleNotAcceptable(
        exchange: ServerWebExchange,
        e: NotAcceptableStatusException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 Accept 입니다", exchange.request.path.value())
        return error(HttpStatus.NOT_ACCEPTABLE, "지원하지 않는 응답 형식입니다")
    }

    // 401
    @ExceptionHandler(AdminUnauthorizedException::class)
    fun handleUnauthorized(
        exchange: ServerWebExchange,
        e: AdminUnauthorizedException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", exchange.request.path.value(), e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    // 403
    @ExceptionHandler(AdminForbiddenException::class)
    fun handleForbidden(
        exchange: ServerWebExchange,
        e: AdminForbiddenException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", exchange.request.path.value(), e.message)
        return error(HttpStatus.FORBIDDEN, e.message ?: "접근 권한이 없습니다")
    }

    // 401
    @ExceptionHandler(InvalidTokenException::class)
    fun handleInvalidToken(
        exchange: ServerWebExchange,
        e: InvalidTokenException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", exchange.request.path.value(), e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "LOGIN_AGAIN")
    }

    // 401
    @ExceptionHandler(ExpiredTokenException::class)
    fun handleExpiredToken(
        exchange: ServerWebExchange,
        e: ExpiredTokenException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", exchange.request.path.value(), e.message)
        return error(HttpStatus.UNAUTHORIZED, e.message ?: "TOKEN_EXPIRED")
    }

    // 400 - 도메인 예외
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        exchange: ServerWebExchange,
        e: IllegalArgumentException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", exchange.request.path.value(), e.message)
        return badRequest(e.message ?: "잘못된 요청입니다")
    }

    // 500 - 도메인 예외
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        exchange: ServerWebExchange,
        e: IllegalStateException
    ): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", exchange.request.path.value(), e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    // 500 - 그 외
    @ExceptionHandler(Exception::class)
    fun handleException(
        exchange: ServerWebExchange,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", exchange.request.path.value(), e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, message)

    private fun error(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> =
        ResponseEntity.status(status).body(
            ErrorResponse(status = status.value(), message = message)
        )
}
