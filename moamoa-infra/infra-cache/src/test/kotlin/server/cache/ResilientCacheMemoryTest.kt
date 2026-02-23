package server.cache

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.EnableAspectJAutoProxy
import org.springframework.context.annotation.Import
import server.config.ResilientCacheProperties
import server.shared.cache.CacheInfraException
import server.shared.cache.CacheMemory
import java.util.function.Supplier

class ResilientCacheMemoryTest {
    @Test
    fun `Redis가 정상일 때는 Redis만 사용한다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k1", String::class.java) } returns "redis"

        withContext(redis, caffeine) { cacheMemory ->
            val result = cacheMemory.get("k1", String::class.java)

            result shouldBe "redis"
            coVerify(exactly = 1) { redis.get("k1", String::class.java) }
            coVerify(exactly = 0) { caffeine.get("k1", String::class.java) }
        }
    }

    @Test
    fun `Redis 실패 시 Caffeine으로 fallback 한다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k1", String::class.java) } throws CacheInfraException("redis down")
        coEvery { caffeine.get("k1", String::class.java) } returns "local"

        withContext(redis, caffeine) { cacheMemory ->
            val result = cacheMemory.get("k1", String::class.java)

            result shouldBe "local"
            coVerify(exactly = 1) { redis.get("k1", String::class.java) }
            coVerify(exactly = 1) { caffeine.get("k1", String::class.java) }
        }
    }

    @Test
    fun `복구 probe 간격 전에는 Redis를 재시도하지 않는다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k1", String::class.java) } throws CacheInfraException("redis down")
        coEvery { caffeine.get("k1", String::class.java) } returns "local"

        withContext(redis, caffeine) { cacheMemory ->
            cacheMemory.get("k1", String::class.java)
            cacheMemory.get("k1", String::class.java)

            coVerify(exactly = 1) { redis.get("k1", String::class.java) }
            coVerify(exactly = 2) { caffeine.get("k1", String::class.java) }
        }
    }

    @Test
    fun `probe 성공 시 Redis로 복귀한다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k1", String::class.java) } throws CacheInfraException("redis down") andThen "redis-recovered" andThen "redis-next"
        coEvery { caffeine.get("k1", String::class.java) } returns "local"

        withContext(redis, caffeine) { cacheMemory ->
            val degraded = cacheMemory.get("k1", String::class.java)
            Thread.sleep(250L)
            val recovered = cacheMemory.get("k1", String::class.java)
            val healthy = cacheMemory.get("k1", String::class.java)

            degraded shouldBe "local"
            recovered shouldBe "redis-recovered"
            healthy shouldBe "redis-next"
            coVerify(exactly = 3) { redis.get("k1", String::class.java) }
            coVerify(exactly = 1) { caffeine.get("k1", String::class.java) }
        }
    }

    @Test
    fun `degrade 상태에서는 write가 Caffeine으로 처리된다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k0", String::class.java) } throws CacheInfraException("redis down")
        coEvery { caffeine.get("k0", String::class.java) } returns "local"
        coEvery { caffeine.set("k1", "v1", 1_000L) } returns Unit
        coEvery { caffeine.mset(mapOf("k2" to 2), 1_000L) } returns Unit
        coEvery { caffeine.evict("k3") } returns Unit

        withContext(redis, caffeine) { cacheMemory ->
            cacheMemory.get("k0", String::class.java)
            cacheMemory.set("k1", "v1", 1_000L)
            cacheMemory.mset(mapOf("k2" to 2), 1_000L)
            cacheMemory.evict("k3")

            coVerify(exactly = 0) { redis.set("k1", "v1", 1_000L) }
            coVerify(exactly = 0) { redis.mset(mapOf("k2" to 2), 1_000L) }
            coVerify(exactly = 0) { redis.evict("k3") }
            coVerify(exactly = 1) { caffeine.set("k1", "v1", 1_000L) }
            coVerify(exactly = 1) { caffeine.mset(mapOf("k2" to 2), 1_000L) }
            coVerify(exactly = 1) { caffeine.evict("k3") }
        }
    }

    @Test
    fun `fallback 실패 시 원래 인프라 예외를 suppressed로 보존한다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        val infraEx = CacheInfraException("redis down")
        coEvery { redis.get("k1", String::class.java) } throws infraEx
        coEvery { caffeine.get("k1", String::class.java) } throws IllegalStateException("local fail")

        withContext(redis, caffeine) { cacheMemory ->
            val ex = shouldThrow<IllegalStateException> {
                cacheMemory.get("k1", String::class.java)
            }

            ex.message shouldBe "local fail"
            containsInfraException(ex, "redis down") shouldBe true
        }
    }

    @Test
    fun `비인프라 예외는 fallback 트리거가 아니다`() = runTest {
        val redis = mockk<CacheMemory>()
        val caffeine = mockk<CacheMemory>()
        coEvery { redis.get("k1", String::class.java) } throws IllegalStateException("business fail")

        withContext(redis, caffeine) { cacheMemory ->
            val ex = shouldThrow<IllegalStateException> {
                cacheMemory.get("k1", String::class.java)
            }

            ex.message shouldBe "business fail"
            coVerify(exactly = 0) { caffeine.get("k1", String::class.java) }
        }
    }

    private inline fun withContext(
        redis: CacheMemory,
        caffeine: CacheMemory,
        block: (cacheMemory: CacheMemory) -> Unit,
    ) {
        AnnotationConfigApplicationContext().use { context ->
            context.registerBean("redisCacheMemory", CacheMemory::class.java, Supplier { redis })
            context.registerBean("caffeineCacheMemory", CacheMemory::class.java, Supplier { caffeine })
            context.registerBean(
                ResilientCacheProperties::class.java,
                Supplier { ResilientCacheProperties(recoveryProbeIntervalMs = 200L) },
            )
            context.register(TestAopConfig::class.java)
            context.refresh()

            block(context.getBean(CacheMemory::class.java))
        }
    }

    @Configuration
    @EnableAspectJAutoProxy(proxyTargetClass = true)
    @Import(
        ResilientCacheMemory::class,
        ResilientCacheAspect::class,
        ResilientCacheRouter::class,
        ResilientCacheMethodInvoker::class,
    )
    internal class TestAopConfig

    private fun containsInfraException(throwable: Throwable?, message: String): Boolean {
        if (throwable == null) return false
        if (throwable is CacheInfraException && throwable.message == message) return true
        if (containsInfraException(throwable.cause, message)) return true
        return throwable.suppressed.any { containsInfraException(it, message) }
    }
}
