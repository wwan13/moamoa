package server.global.logging

import io.github.oshai.kotlinlogging.KLogger
import kotlin.coroutines.Continuation

suspend inline fun KLogger.infoWithTrace(crossinline message: () -> String) {
    val context = RequestLogContextHolder.current()
    infoWithTraceId(context?.traceId) { message() }
}

suspend inline fun KLogger.warnWithTrace(
    throwable: Throwable? = null,
    crossinline message: () -> String
) {
    val context = RequestLogContextHolder.current()
    warnWithTraceId(context?.traceId, throwable) { message() }
}

inline fun KLogger.infoWithTraceId(traceId: String?, crossinline message: () -> String) {
    RequestLogContextHolder.withTraceId(traceId) {
        info { message() }
    }
}

inline fun KLogger.warnWithTraceId(
    traceId: String?,
    throwable: Throwable? = null,
    crossinline message: () -> String
) {
    RequestLogContextHolder.withTraceId(traceId) {
        if (throwable == null) {
            warn { message() }
        } else {
            warn(throwable) { message() }
        }
    }
}

inline fun KLogger.errorWithTraceId(
    traceId: String?,
    throwable: Throwable? = null,
    crossinline message: () -> String
) {
    RequestLogContextHolder.withTraceId(traceId) {
        if (throwable == null) {
            error { message() }
        } else {
            error(throwable) { message() }
        }
    }
}

inline fun KLogger.info(continuation: Continuation<*>, crossinline message: () -> String) {
    val traceId = RequestLogContextHolder.fromContinuation(continuation)?.traceId
    infoWithTraceId(traceId) { message() }
}

inline fun KLogger.warn(
    continuation: Continuation<*>,
    throwable: Throwable? = null,
    crossinline message: () -> String
) {
    val traceId = RequestLogContextHolder.fromContinuation(continuation)?.traceId
    warnWithTraceId(traceId, throwable) { message() }
}

inline fun KLogger.error(
    continuation: Continuation<*>,
    throwable: Throwable? = null,
    crossinline message: () -> String
) {
    val traceId = RequestLogContextHolder.fromContinuation(continuation)?.traceId
    errorWithTraceId(traceId, throwable) { message() }
}
