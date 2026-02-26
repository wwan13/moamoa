package server.global.logging

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KLogger
import org.slf4j.MDC
import kotlin.coroutines.Continuation

val KLogger.request: TypedLogger
    get() = TypedLogger(this, LogType.REQUEST)

val KLogger.redis: TypedLogger
    get() = TypedLogger(this, LogType.REDIS)

val KLogger.db: TypedLogger
    get() = TypedLogger(this, LogType.DB)

val KLogger.api: TypedLogger
    get() = TypedLogger(this, LogType.API)

val KLogger.event: EventTypedLogger
    get() = EventTypedLogger(TypedLogger(this, LogType.EVENT))

val KLogger.errorType: TypedLogger
    get() = TypedLogger(this, LogType.ERROR)

class TypedLogger internal constructor(
    private val logger: KLogger,
    private val type: LogType,
) {
    fun info(
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = null,
            level = StructuredLevel.INFO,
            throwable = null,
            fields = fields,
            logMessage = message,
        )
    }

    fun info(
        traceId: String?,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) = infoWithTraceId(traceId, *fields, message = message)

    fun infoWithTraceId(
        traceId: String?,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = traceId,
            level = StructuredLevel.INFO,
            throwable = null,
            fields = fields,
            logMessage = message,
        )
    }

    fun warn(
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = null,
            level = StructuredLevel.WARN,
            throwable = null,
            fields = fields,
            logMessage = message,
        )
    }

    fun warn(
        traceId: String?,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) = warnWithTraceId(traceId, *fields, message = message)

    fun warnWithTraceId(
        traceId: String?,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = traceId,
            level = StructuredLevel.WARN,
            throwable = null,
            fields = fields,
            logMessage = message,
        )
    }

    fun warn(
        throwable: Throwable,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = null,
            level = StructuredLevel.WARN,
            throwable = throwable,
            fields = fields,
            logMessage = message,
        )
    }

    fun warn(
        traceId: String?,
        throwable: Throwable,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) = warnWithTraceId(traceId, throwable, *fields, message = message)

    fun warnWithTraceId(
        traceId: String?,
        throwable: Throwable,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = traceId,
            level = StructuredLevel.WARN,
            throwable = throwable,
            fields = fields,
            logMessage = message,
        )
    }

    fun error(
        throwable: Throwable? = null,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = null,
            level = StructuredLevel.ERROR,
            throwable = throwable,
            fields = fields,
            logMessage = message,
        )
    }

    fun error(
        traceId: String?,
        throwable: Throwable? = null,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) = errorWithTraceId(traceId, throwable, *fields, message = message)

    fun errorWithTraceId(
        traceId: String?,
        throwable: Throwable? = null,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        log(
            traceId = traceId,
            level = StructuredLevel.ERROR,
            throwable = throwable,
            fields = fields,
            logMessage = message,
        )
    }

    private fun log(
        traceId: String?,
        level: StructuredLevel,
        throwable: Throwable?,
        fields: Array<out Pair<String, Any?>>,
        logMessage: () -> String,
    ) {
        val effectiveTraceId = traceId
            ?.takeUnless { it.isBlank() }
            ?: MDC.get("traceId")
            ?: RequestLogContextHolder.SYSTEM_TRACE_ID

        RequestLogContextHolder.withTraceId(effectiveTraceId) {
            val data = linkedMapOf<String, Any?>()
            fields.forEach { (key, value) ->
                if (value != null) {
                    data[key] = value
                }
            }
            val payload = mapOf(
                "type" to type.name,
                "data" to data,
            )

            when (level) {
                StructuredLevel.INFO -> logger.atInfo {
                    message = logMessage()
                    this.payload = payload
                    if (throwable != null) cause = throwable
                }

                StructuredLevel.WARN -> logger.atWarn {
                    message = logMessage()
                    this.payload = payload
                    if (throwable != null) cause = throwable
                }

                StructuredLevel.ERROR -> logger.atError {
                    message = logMessage()
                    this.payload = payload
                    if (throwable != null) cause = throwable
                }
            }
        }
    }
}

class EventTypedLogger internal constructor(
    private val typedLogger: TypedLogger,
) {
    suspend fun info(
        event: Any,
        message: () -> String,
    ) {
        val traceId = RequestLogContextHolder.current()?.traceId
        typedLogger.infoWithTraceId(traceId, *eventFields(event)) {
            message()
        }
    }

    suspend fun info(
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        val traceId = RequestLogContextHolder.current()?.traceId
        typedLogger.infoWithTraceId(
            traceId,
            *eventFields("NONE", fields),
            message = message
        )
    }

    fun infoWithTraceId(
        traceId: String?,
        vararg fields: Pair<String, Any?>,
        message: () -> String,
    ) {
        typedLogger.infoWithTraceId(
            traceId,
            *eventFields("NONE", fields),
            message = message
        )
    }

    fun infoWithTraceId(
        traceId: String?,
        event: Any,
        message: () -> String,
    ) {
        typedLogger.infoWithTraceId(traceId, *eventFields(event)) { message() }
    }

    private fun eventFields(event: Any): Array<out Pair<String, Any?>> {
        val map = objectMapper.convertValue(
            event,
            object : TypeReference<LinkedHashMap<String, Any?>>() {}
        )
        return map.entries.map { it.key to it.value }.toTypedArray()
    }

    private fun eventFields(
        eventName: String,
        fields: Array<out Pair<String, Any?>>,
    ): Array<out Pair<String, Any?>> =
        arrayOf("event" to eventName, *fields)

    private companion object {
        val objectMapper = jacksonObjectMapper()
    }
}

private enum class StructuredLevel {
    INFO,
    WARN,
    ERROR,
}

fun KLogger.traceIdFrom(continuation: Continuation<*>): String? =
    RequestLogContextHolder.fromContinuation(continuation)?.traceId
