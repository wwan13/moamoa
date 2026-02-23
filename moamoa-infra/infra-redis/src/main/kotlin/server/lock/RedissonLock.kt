package server.lock

import org.redisson.api.RLock
import org.redisson.api.RedissonClient
import org.springframework.stereotype.Component
import server.async.await
import server.shared.lock.KeyedLock
import server.shared.lock.LockInfraException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

@Component("redissonLock")
internal class RedissonLock(
    private val redissonClient: RedissonClient,
) : KeyedLock {

    override suspend fun <T> withLock(key: String, block: suspend () -> T): T {
        val lockName = "$LOCK_NAMESPACE:$key"
        val lock = redissonClient.getLock(lockName)
        val lockOwnerId = lockOwnerIdSequence.incrementAndGet()

        lock(lock, lockName, lockOwnerId)
        val blockResult = runCatching { block() }
        unlock(lock, lockName, lockOwnerId, blockResult.exceptionOrNull())

        return blockResult.getOrThrow()
    }

    private suspend fun lock(lock: RLock, lockName: String, lockOwnerId: Long) {
        val acquire = try {
            lock.tryLockAsync(LOCK_WAIT_MILLIS, LOCK_LEASE_MILLIS, TimeUnit.MILLISECONDS, lockOwnerId).await()
        } catch (ex: Throwable) {
            throw LockInfraException("Distributed lock acquire failed. key=$lockName", ex)
        }

        if (!acquire) {
            throw IllegalStateException("Distributed lock not acquired. key=$lockName")
        }
    }

    private suspend fun unlock(
        lock: RLock,
        lockName: String,
        lockOwnerId: Long,
        blockFailure: Throwable?,
    ) {
        try {
            lock.unlockAsync(lockOwnerId).await()
        } catch (ex: Throwable) {
            val unlockFailure = LockInfraException("Distributed lock release failed. key=$lockName", ex)
            if (blockFailure == null) {
                throw unlockFailure
            }
            blockFailure.addSuppressed(unlockFailure)
        }
    }

    private companion object {
        private const val LOCK_NAMESPACE = "moamoa:lock"
        private const val LOCK_WAIT_MILLIS = 3_000L
        private const val LOCK_LEASE_MILLIS = 10_000L

        private val lockOwnerIdSequence = AtomicLong(0)
    }
}
