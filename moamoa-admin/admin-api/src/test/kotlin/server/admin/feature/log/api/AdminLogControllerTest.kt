package server.admin.feature.log.api

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.core.MethodParameter
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.support.WebDataBinderFactory
import org.springframework.web.context.request.NativeWebRequest
import org.springframework.web.method.support.HandlerMethodArgumentResolver
import org.springframework.web.method.support.ModelAndViewContainer
import server.admin.feature.log.query.AdminLogCursor
import server.admin.feature.log.query.AdminLogPage
import server.admin.feature.log.query.AdminLogQueryConditions
import server.admin.feature.log.query.AdminLogQueryService
import server.admin.feature.log.query.AdminLogSummary
import server.admin.feature.member.domain.AdminMemberRole
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport
import server.admin.global.web.AdminApiControllerAdvice
import test.UnitTest
import java.time.LocalDateTime

class AdminLogControllerTest : UnitTest() {

    @Test
    fun `관리자 권한이 없으면 403을 반환한다`() {
        val service = mockk<AdminLogQueryService>()
        val controller = AdminLogController(service)
        val mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(AdminApiControllerAdvice())
            .setCustomArgumentResolvers(FixedPassportResolver(AdminPassport(1L, AdminMemberRole.USER)))
            .build()

        mockMvc.get("/api/admin/log")
            .andExpect {
                status { isForbidden() }
            }

        verify(exactly = 0) { service.findByConditions(any()) }
    }

    @Test
    fun `요청 파라미터 타입이 잘못되면 400을 반환한다`() {
        val service = mockk<AdminLogQueryService>()
        val controller = AdminLogController(service)
        val mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(AdminApiControllerAdvice())
            .setCustomArgumentResolvers(FixedPassportResolver(AdminPassport(1L, AdminMemberRole.ADMIN)))
            .build()

        mockMvc.get("/api/admin/log") {
            param("size", "INVALID")
        }.andExpect {
            status { isBadRequest() }
        }

        verify(exactly = 0) { service.findByConditions(any()) }
    }

    @Test
    fun `정상 요청 시 로그 목록 응답을 반환한다`() {
        val service = mockk<AdminLogQueryService>()
        val controller = AdminLogController(service)
        val mockMvc = MockMvcBuilders
            .standaloneSetup(controller)
            .setControllerAdvice(AdminApiControllerAdvice())
            .setCustomArgumentResolvers(FixedPassportResolver(AdminPassport(1L, AdminMemberRole.ADMIN)))
            .build()
        val now = LocalDateTime.of(2026, 3, 12, 23, 0, 0)
        val response = AdminLogPage(
            items = listOf(
                AdminLogSummary(
                    id = 100L,
                    timestamp = now,
                    logLevel = "INFO",
                    traceId = "trace-1",
                    loggerName = "logger",
                    message = "message",
                    type = "API",
                    data = "{}",
                )
            ),
            nextCursor = AdminLogCursor(timestamp = now, id = 100L),
            size = 100L,
            hasNext = true,
        )
        every { service.findByConditions(any()) } returns response

        mockMvc.get("/api/admin/log") {
            contentType = MediaType.APPLICATION_JSON
            param("logLevel", "INFO")
            param("type", "API")
            param("traceId", "trace")
            param("size", "10")
        }.andExpect {
            status { isOk() }
            jsonPath("$.size") { value(100) }
            jsonPath("$.hasNext") { value(true) }
            jsonPath("$.items[0].id") { value(100) }
            jsonPath("$.items[0].logLevel") { value("INFO") }
            jsonPath("$.items[0].type") { value("API") }
        }

        verify(exactly = 1) {
            service.findByConditions(
                AdminLogQueryConditions(
                    logLevel = "INFO",
                    type = "API",
                    traceId = "trace",
                    size = 10L,
                    cursorTimestamp = null,
                    cursorId = null,
                )
            )
        }
    }

    private class FixedPassportResolver(
        private val passport: AdminPassport,
    ) : HandlerMethodArgumentResolver {
        override fun supportsParameter(parameter: MethodParameter): Boolean {
            return parameter.hasParameterAnnotation(RequestAdminPassport::class.java) &&
                parameter.parameterType == AdminPassport::class.java
        }

        override fun resolveArgument(
            parameter: MethodParameter,
            mavContainer: ModelAndViewContainer?,
            webRequest: NativeWebRequest,
            binderFactory: WebDataBinderFactory?,
        ): Any = passport
    }
}
