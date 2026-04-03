package server.admin.global.web

import org.springframework.http.HttpStatus
import org.springframework.http.HttpStatusCode
import org.springframework.http.ResponseEntity

internal class AdminApiResponse<T> private constructor(
    body: T?,
    status: HttpStatusCode,
) : ResponseEntity<T>(body, status) {

    companion object {
        fun <T> of(
            body: T,
            status: HttpStatusCode = HttpStatus.OK
        ): AdminApiResponse<T> {
            return AdminApiResponse(body, status)
        }

        fun <T> of(
            status: HttpStatusCode = HttpStatus.OK
        ): AdminApiResponse<T> {
            return AdminApiResponse(null, status)
        }

        fun <T> error(
            body: T,
            status: HttpStatusCode = HttpStatus.INTERNAL_SERVER_ERROR
        ): AdminApiResponse<T> {
            return AdminApiResponse(body, status)
        }
    }
}
