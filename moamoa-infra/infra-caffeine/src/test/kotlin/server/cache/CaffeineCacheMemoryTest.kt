package server.cache

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.maps.shouldContainExactly
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

class CaffeineCacheMemoryTest {
    private val cacheMemory = CaffeineCacheMemory(jacksonObjectMapper())

    @Test
    fun `set 후 get은 저장된 값을 반환한다`() = runTest {
        cacheMemory.set("k1", User("u1", 20), null)

        val result = cacheMemory.get("k1", User::class.java)

        result shouldBe User("u1", 20)
    }

    @Test
    fun `TTL이 지나면 get은 null을 반환한다`() = runTest {
        cacheMemory.set("k1", User("u1", 20), 10L)
        Thread.sleep(20L)

        val result = cacheMemory.get("k1", User::class.java)

        result.shouldBeNull()
    }

    @Test
    fun `setIfAbsent는 기존 값이 없을 때만 저장한다`() = runTest {
        val first = cacheMemory.setIfAbsent("k1", "v1", null)
        val second = cacheMemory.setIfAbsent("k1", "v2", null)
        val value = cacheMemory.get("k1", String::class.java)

        first shouldBe true
        second shouldBe false
        value shouldBe "v1"
    }

    @Test
    fun `incr와 decrBy는 숫자 값을 증감한다`() = runTest {
        val incr = cacheMemory.incr("count")
        val decr = cacheMemory.decrBy("count", 3L)

        incr shouldBe 1L
        decr shouldBe -2L
    }

    @Test
    fun `mset과 mget은 키별 값을 조회한다`() = runTest {
        cacheMemory.mset(mapOf("k1" to 1, "k2" to 2), null)

        val result = cacheMemory.mget(listOf("k1", "k2", "k3"))

        result.shouldContainExactly(
            mapOf(
                "k1" to "1",
                "k2" to "2",
                "k3" to null,
            )
        )
    }

    @Test
    fun `mgetAs는 타입으로 역직렬화한다`() = runTest {
        val users = listOf(User("u1", 20), User("u2", 30))
        cacheMemory.set("users", users, null)

        val result = cacheMemory.mgetAs(
            keys = listOf("users", "missing"),
            typeRef = object : TypeReference<List<User>>() {},
        )

        result["users"]?.shouldContainExactly(users)
        result["missing"].shouldBeNull()
    }

    @Test
    fun `evictByPrefix는 prefix로 시작하는 키만 삭제한다`() = runTest {
        cacheMemory.set("user:1", "a", null)
        cacheMemory.set("user:2", "b", null)
        cacheMemory.set("post:1", "c", null)

        cacheMemory.evictByPrefix("user:")

        cacheMemory.get("user:1", String::class.java).shouldBeNull()
        cacheMemory.get("user:2", String::class.java).shouldBeNull()
        cacheMemory.get("post:1", String::class.java) shouldBe "c"
    }

    @Test
    fun `TTL이 있는 mset은 만료 후 조회되지 않는다`() = runTest {
        cacheMemory.mset(mapOf("k1" to 1, "k2" to 2), 10L)
        Thread.sleep(20L)

        val result = cacheMemory.mget(listOf("k1", "k2"))

        result.shouldContainExactly(
            mapOf(
                "k1" to null,
                "k2" to null,
            )
        )
    }

    @Test
    fun `최대 엔트리 수를 초과하면 eviction이 발생한다`() = runTest {
        val limitedCacheMemory = CaffeineCacheMemory(jacksonObjectMapper(), maximumSize = 2L)
        limitedCacheMemory.set("k1", "v1", null)
        limitedCacheMemory.set("k2", "v2", null)
        limitedCacheMemory.set("k3", "v3", null)

        limitedCacheMemory.estimatedSize() shouldBe 2L
    }

    private data class User(
        val name: String,
        val age: Int,
    )
}
