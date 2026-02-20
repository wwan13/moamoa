package server.messaging

import org.springframework.stereotype.Component
import server.config.SubscriptionReaderProperties

@Component
internal class StreamConnectionStateManager(
    private val properties: SubscriptionReaderProperties,
) {
    internal enum class ReaderMode { ACTIVE, DEGRADED }

    internal data class ReaderState(
        val mode: ReaderMode = ReaderMode.ACTIVE,
        val nextProbeAtMillis: Long = 0L,
    )

    fun initialState(): ReaderState = ReaderState()

    fun shouldWaitBeforeProbe(state: ReaderState, nowMillis: Long): Long? {
        if (state.mode != ReaderMode.DEGRADED) return null
        if (nowMillis >= state.nextProbeAtMillis) return null
        return minOf(state.nextProbeAtMillis - nowMillis, 1_000L)
    }

    fun enterDegraded(nowMillis: Long): ReaderState =
        ReaderState(
            mode = ReaderMode.DEGRADED,
            nextProbeAtMillis = nowMillis + readPauseOnFailureMillis(),
        )

    fun onProbeFailed(nowMillis: Long): ReaderState =
        ReaderState(
            mode = ReaderMode.DEGRADED,
            nextProbeAtMillis = nowMillis + recoveryProbeIntervalMillis(),
        )

    fun recover(): ReaderState = ReaderState(mode = ReaderMode.ACTIVE, nextProbeAtMillis = 0L)

    fun readPauseOnFailureMillis(): Long = properties.readPauseOnFailureMs.coerceAtLeast(1L)

    fun recoveryProbeIntervalMillis(): Long = properties.recoveryProbeIntervalMs.coerceAtLeast(1L)
}
