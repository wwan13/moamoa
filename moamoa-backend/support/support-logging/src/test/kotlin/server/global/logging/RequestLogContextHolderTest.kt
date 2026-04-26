package server.global.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.slf4j.MDC
import test.UnitTest

class RequestLogContextHolderTest : UnitTest() {

    @AfterEach
    fun clearMdc() {
        MDC.clear()
    }

    @Test
    fun `withTraceId는 기존 traceId를 원복한다`() {
        MDC.put("traceId", "request123")

        RequestLogContextHolder.withTraceId("override456") {
            assertEquals("override456", MDC.get("traceId"))
        }

        assertEquals("request123", MDC.get("traceId"))
    }

    @Test
    fun `withContext는 기존 MDC를 원복한다`() {
        MDC.put("traceId", "parentTrace")
        MDC.put("userId", "42")
        MDC.put("clientIp", "127.0.0.1")
        val context = RequestLogContext(
            traceId = "childTrace",
            userId = 99L,
            clientIp = "10.0.0.1",
        )

        RequestLogContextHolder.withContext(context) {
            assertEquals("childTrace", MDC.get("traceId"))
            assertEquals("99", MDC.get("userId"))
            assertEquals("10.0.0.1", MDC.get("clientIp"))
        }

        assertEquals("parentTrace", MDC.get("traceId"))
        assertEquals("42", MDC.get("userId"))
        assertEquals("127.0.0.1", MDC.get("clientIp"))
    }

    @Test
    fun `withContext는 비어있던 userId와 clientIp를 실행 후 제거한다`() {
        MDC.put("traceId", "parentTrace")
        val context = RequestLogContext(
            traceId = "childTrace",
            userId = null,
            clientIp = null,
        )

        RequestLogContextHolder.withContext(context) {
            assertEquals("childTrace", MDC.get("traceId"))
            assertNull(MDC.get("userId"))
            assertNull(MDC.get("clientIp"))
        }

        assertEquals("parentTrace", MDC.get("traceId"))
        assertNull(MDC.get("userId"))
        assertNull(MDC.get("clientIp"))
    }
}
