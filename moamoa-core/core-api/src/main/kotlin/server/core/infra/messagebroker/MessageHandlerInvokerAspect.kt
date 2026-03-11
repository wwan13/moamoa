package server.core.infra.messagebroker

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.slf4j.MDC
import org.springframework.stereotype.Component
import server.global.logging.event

@Aspect
@Component
class MessageHandlerInvokerAspect {
    private val logger = KotlinLogging.logger {}

    @Around(
        "execution(* server.messaging.MessageHandlerInvoker.invoke(..)) && args(eventId, type, payload, ..)"
    )
    fun aroundInvoke(
        joinPoint: ProceedingJoinPoint,
        eventId: String?,
        type: String?,
        payload: Any,
    ): Any? {
        logger.event.info(
            payload,
            "type" to (type ?: payload::class.simpleName),
            "eventId" to (eventId ?: "MISSING"),
        ) { "메시지 핸들러를 실행합니다" }

        if (eventId.isNullOrBlank()) {
            return joinPoint.proceed()
        }

        val closeable = MDC.putCloseable("eventId", eventId)
        return try {
            joinPoint.proceed()
        } finally {
            closeable.close()
        }
    }
}
