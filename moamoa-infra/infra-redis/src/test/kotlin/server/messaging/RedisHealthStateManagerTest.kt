package server.messaging

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.springframework.data.redis.RedisConnectionFailureException
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.StringRedisTemplate
import server.messaging.health.RedisHealthStateManager
import server.messaging.health.RedisRecoveryAction
import server.messaging.health.RedisRecoveryActionRunner

class RedisHealthStateManagerTest {

    @Test
    fun `ACTIVE 상태에서는 runSafe가 람다를 실행하고 결과를 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"
        val manager = newManager(redis)
        var executedBlock = false

        val result = manager.runSafe {
            executedBlock = true
            "ok"
        }

        result.getOrNull() shouldBe "ok"
        executedBlock shouldBe true
    }

    @Test
    fun `DEGRADED 상태에서는 runSafe가 람다를 실행하지 않고 null을 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } throws RedisConnectionFailureException("redis down")
        val manager = newManager(redis)
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
    }

    @Test
    fun `RedisConnectionFailureException 발생 후 tryRecover 성공 시 runSafe가 재실행된다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } returns "PONG"
        var recoveryActionCalled = false
        val action = RedisRecoveryAction { recoveryActionCalled = true }
        val manager = newManager(
            redis = redis,
            actions = listOf(action),
        )
        manager.runSafe {
            throw RedisConnectionFailureException("redis down")
        }
        manager.tryRecover() shouldBe true

        var executedBlock = false
        val result = manager.runSafe {
            executedBlock = true
            "ok"
        }

        result.getOrNull() shouldBe "ok"
        executedBlock shouldBe true
        recoveryActionCalled shouldBe true
    }

    @Test
    fun `이미 DEGRADED 상태에서 runSafe 재호출 시 계속 실패를 반환한다`() = runBlocking {
        val redis = mockk<StringRedisTemplate>()
        every { redis.execute(any<RedisCallback<String>>()) } throws RedisConnectionFailureException("redis down")
        val manager = newManager(redis)
        manager.runSafe {
            throw RedisConnectionFailureException("redis down")
        }

        manager.runSafe {
            throw RedisConnectionFailureException("redis down")
        }.isFailure shouldBe true
    }

    private fun newManager(
        redis: StringRedisTemplate,
        actions: List<RedisRecoveryAction> = emptyList(),
    ): RedisHealthStateManager {
        return RedisHealthStateManager(
            redis = redis,
            recoveryActionRunner = mockk<RedisRecoveryActionRunner>(relaxed = true).also { runner ->
                coEvery { runner.runAll() } answers { runBlocking { actions.forEach { it.onRecovered() } } }
            },
        )
    }
}
