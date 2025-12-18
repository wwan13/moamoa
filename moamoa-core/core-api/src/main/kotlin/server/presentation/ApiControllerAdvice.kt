package server.presentation

import jakarta.servlet.http.HttpServletRequest
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.validation.BindException
import org.springframework.web.HttpRequestMethodNotSupportedException
import org.springframework.web.bind.MissingServletRequestParameterException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException
import org.springframework.web.servlet.NoHandlerFoundException

@RestControllerAdvice
class ApiControllerAdvice {

    private val log = LoggerFactory.getLogger(javaClass)

    data class ErrorResponse(
        val status: Int,
        val message: String
    )

    // 400 - @ModelAttribute / @RequestParam 바인딩 검증 실패
    @ExceptionHandler(BindException::class)
    fun handleBindException(
        request: HttpServletRequest,
        e: BindException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 값이 올바르지 않습니다", request.requestURI, e)
        return badRequest("요청 값이 올바르지 않습니다")
    }

    // 400 - JSON 파싱 실패(잘못된 body)
    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleNotReadable(
        request: HttpServletRequest,
        e: HttpMessageNotReadableException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 본문(JSON)이 올바르지 않습니다", request.requestURI, e)
        return badRequest("요청 본문(JSON)이 올바르지 않습니다")
    }

    // 400 - 파라미터 타입 미스매치
    @ExceptionHandler(MethodArgumentTypeMismatchException::class)
    fun handleTypeMismatch(
        request: HttpServletRequest,
        e: MethodArgumentTypeMismatchException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청 값 형식이 올바르지 않습니다", request.requestURI, e)
        return badRequest("요청 값 형식이 올바르지 않습니다")
    }

    // 400 - 필수 쿼리 파라미터 누락
    @ExceptionHandler(MissingServletRequestParameterException::class)
    fun handleMissingParam(
        request: HttpServletRequest,
        e: MissingServletRequestParameterException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 필수 파라미터가 누락되었습니다: {}", request.requestURI, e.parameterName, e)
        return badRequest("필수 파라미터가 누락되었습니다: ${e.parameterName}")
    }

    // 404 - 핸들러 없음
    @ExceptionHandler(NoHandlerFoundException::class)
    fun handleNoHandlerFound(
        request: HttpServletRequest,
        e: NoHandlerFoundException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 요청한 리소스를 찾을 수 없습니다", request.requestURI, e)
        return error(HttpStatus.NOT_FOUND, "요청한 리소스를 찾을 수 없습니다")
    }

    // 405 - 메소드 미지원
    @ExceptionHandler(HttpRequestMethodNotSupportedException::class)
    fun handleMethodNotSupported(
        request: HttpServletRequest,
        e: HttpRequestMethodNotSupportedException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] 지원하지 않는 HTTP 메서드입니다", request.requestURI, e)
        return error(HttpStatus.METHOD_NOT_ALLOWED, "지원하지 않는 HTTP 메서드입니다")
    }

    // 400 - 도메인 예외
    @ExceptionHandler(IllegalArgumentException::class)
    fun handleIllegalArgument(
        request: HttpServletRequest,
        e: IllegalArgumentException
    ): ResponseEntity<ErrorResponse> {
        log.warn("[{}] {}", request.requestURI, e.message, e)
        return badRequest(e.message ?: "잘못된 요청입니다")
    }

    // 500 - 도메인 예외
    @ExceptionHandler(IllegalStateException::class)
    fun handleIllegalState(
        request: HttpServletRequest,
        e: IllegalStateException
    ): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", request.requestURI, e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    // 500 - 그 외
    @ExceptionHandler(Exception::class)
    fun handleException(
        request: HttpServletRequest,
        e: Exception
    ): ResponseEntity<ErrorResponse> {
        log.error("[{}] {}", request.requestURI, e.message, e)
        return error(HttpStatus.INTERNAL_SERVER_ERROR, "서버 오류가 발생했습니다")
    }

    private fun badRequest(message: String): ResponseEntity<ErrorResponse> =
        error(HttpStatus.BAD_REQUEST, message)

    private fun error(status: HttpStatus, message: String): ResponseEntity<ErrorResponse> {
        val errorResponse = ErrorResponse(
            status = status.value(),
            message = message
        )
        return ResponseEntity.status(status).body(errorResponse)
    }
}