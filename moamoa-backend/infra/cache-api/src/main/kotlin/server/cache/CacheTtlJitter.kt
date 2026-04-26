package server.cache

import kotlin.random.Random

private const val JITTER_MAX_RATIO = 0.1

fun ttlWithJitter(ttlMillis: Long?): Long? {
    if (ttlMillis == null) return null
    if (ttlMillis <= 0L) return ttlMillis

    val jitterBound = (ttlMillis * JITTER_MAX_RATIO).toLong()
    if (jitterBound <= 0L) return ttlMillis

    return ttlMillis + Random.nextLong(jitterBound + 1)
}
