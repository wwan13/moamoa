package server.messaging.logging

import io.github.oshai.kotlinlogging.KotlinLogging
import org.aspectj.lang.ProceedingJoinPoint
import org.aspectj.lang.annotation.Around
import org.aspectj.lang.annotation.Aspect
import org.springframework.stereotype.Component
import server.global.logging.RequestLogContextHolder
import server.global.logging.errorType
import server.global.logging.redis
import kotlin.time.TimeSource

@Aspect
@Component
internal class EventPublisherLoggingAspect {
    private val logger = KotlinLogging.logger {}

    @Around(
        "execution(* server.messaging.EventPublisher.publish(..)) && args(channel, type, payloadJson, eventId)"
    )
    fun aroundPublish(
        joinPoint: ProceedingJoinPoint,
        channel: String,
        type: String,
        payloadJson: String,
        eventId: String,
    ): Any? {
        val mark = TimeSource.Monotonic.markNow()
        val context = RequestLogContextHolder.current()
        return try {
            joinPoint.proceed().also {
                logger.redis.info(
                    traceId = context?.traceId,
                    "call" to "EventPublisher.publish",
                    "channel" to channel,
                    "eventType" to type,
                    "eventId" to eventId,
                    "latencyMs" to mark.elapsedNow().inWholeMilliseconds,
                    "success" to true,
                ) {
                    "이벤트 발행이 성공했습니다"
                }
            }
        } catch (error: Throwable) {
            val errorType = error::class.simpleName ?: "UnknownException"
            val errorSummary = error.message?.replace(Regex("\\s+"), " ")?.take(180) ?: "unknown"
            logger.redis.warn(
                traceId = context?.traceId,
                throwable = error,
                "call" to "EventPublisher.publish",
                "channel" to channel,
                "eventType" to type,
                "eventId" to eventId,
                "latencyMs" to mark.elapsedNow().inWholeMilliseconds,
                "success" to false,
                "errorType" to errorType,
            ) {
                "이벤트 발행이 실패했습니다"
            }
            logger.errorType.warn(
                traceId = context?.traceId,
                throwable = error,
                "call" to "EventPublisher.publish",
                "errorType" to errorType,
                "message" to errorSummary,
            ) {
                "이벤트 발행 오류가 발생했습니다"
            }
            throw error
        }
    }
}
