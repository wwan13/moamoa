package server.messaging

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import server.config.RedisHealthProperties
import server.messaging.health.RedisHealthStateManager
import server.messaging.health.RedisRecoveryAction
import server.messaging.health.RedisRecoveryActionRunner

class RedisHealthStateManagerTest {

    @Test
    fun `ACTIVE 상태에서는 runSafe가 람다를 실행하고 결과를 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"
        val (manager, scope) = newManager(redis)
        try {
            var executedBlock = false

            val result = manager.runSafe {
                executedBlock = true
                "ok"
            }

            result.getOrNull() shouldBe "ok"
            executedBlock shouldBe true
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `DEGRADED 상태에서는 runSafe가 람다를 실행하지 않고 null을 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } throws RedisConnectionFailureException("redis down")
        val (manager, scope) = newManager(redis, pauseMs = 1_000, probeIntervalMs = 1_000)
        try {
            manager.runSafe {
                throw RedisConnectionFailureException("redis down")
            }

            var executedBlock = false
            val result = manager.runSafe {
                executedBlock = true
                "ok"
            }

            result.isFailure shouldBe true
            executedBlock shouldBe false
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `RedisConnectionFailureException 발생 시 복구 루프가 동작하고 복구 후 runSafe가 재실행된다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"
        var recoveryActionCalled = false
        val action = RedisRecoveryAction { recoveryActionCalled = true }
        val (manager, scope) = newManager(
            redis = redis,
            pauseMs = 20,
            probeIntervalMs = 20,
            actions = listOf(action),
        )
        try {
            manager.runSafe {
                throw RedisConnectionFailureException("redis down")
            }

            Thread.sleep(120)

            var executedBlock = false
            val result = manager.runSafe {
                executedBlock = true
                "ok"
            }

            result.getOrNull() shouldBe "ok"
            executedBlock shouldBe true
            recoveryActionCalled shouldBe true
        } finally {
            scope.cancel()
        }
    }

    @Test
    fun `이미 DEGRADED 상태에서 runSafe 재호출 시 계속 실패를 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } throws RedisConnectionFailureException("redis down")
        val (manager, scope) = newManager(redis, pauseMs = 1_000, probeIntervalMs = 1_000)
        try {
            manager.runSafe {
                throw RedisConnectionFailureException("redis down")
            }

            manager.runSafe {
                throw RedisConnectionFailureException("redis down")
            }.isFailure shouldBe true
        } finally {
            scope.cancel()
        }
    }

    private fun newManager(
        redis: StringRedisTemplate,
        pauseMs: Long = 30_000,
        probeIntervalMs: Long = 30_000,
        actions: List<RedisRecoveryAction> = emptyList(),
    ): Pair<RedisHealthStateManager, CoroutineScope> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val properties = RedisHealthProperties(
            pauseOnFailureMs = pauseMs,
            recoveryProbeIntervalMs = probeIntervalMs,
        )
        return RedisHealthStateManager(
            properties = properties,
            schedulerScope = scope,
            redis = redis,
            recoveryActionRunner = mockk<RedisRecoveryActionRunner>(relaxed = true).also { runner ->
                coEvery { runner.runAll() } answers { runBlocking { actions.forEach { it.onRecovered() } } }
            },
        ) to scope
    }
}
