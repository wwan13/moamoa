package server.cache

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.reactive.awaitSingle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.reactivestreams.Publisher
import org.springframework.data.redis.connection.ReactiveRedisConnection
import org.springframework.data.redis.connection.ReactiveStringCommands
import org.springframework.data.redis.connection.ReactiveStringCommands.SetCommand
import org.springframework.data.redis.connection.RedisStringCommands.SetOption
import org.springframework.data.redis.core.ReactiveRedisCallback
import org.springframework.data.redis.core.ReactiveRedisTemplate
import org.springframework.data.redis.core.ReactiveValueOperations
import org.yaml.snakeyaml.util.UriEncoder.decode
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import java.nio.ByteBuffer
import java.nio.charset.StandardCharsets

class CacheMemoryTest {
    private val redisTemplate = mockk<ReactiveRedisTemplate<String, String>>()
    private val valueOps = mockk<ReactiveValueOperations<String, String>>()
    private val connection = mockk<ReactiveRedisConnection>()
    private val stringCommands = mockk<ReactiveStringCommands>()
    private val objectMapper = jacksonObjectMapper()
    private val cacheMemory = CacheMemory(redisTemplate, objectMapper)

    init {
        every { connection.stringCommands() } returns stringCommands
    }

    @Test
    fun `mget은 키 목록이 비어있으면 빈 맵을 반환한다`() = runTest {
        val result = cacheMemory.mget(emptyList())

        result shouldBe emptyMap()
        verify(exactly = 0) { redisTemplate.opsForValue() }
        verify(exactly = 0) { redisTemplate.execute(any<ReactiveRedisCallback<List<String?>>>()) }
    }

    @Test
    fun `mget은 네이티브 MGET 결과를 키 순서대로 매핑한다`() = runTest {
        mockExecuteWithConnection<List<String?>>()
        val keys = listOf("k1", "k2")
        val capturedKeys = slot<List<ByteBuffer>>()

        every { stringCommands.mGet(capture(capturedKeys)) } returns Mono.just(listOf(encode("v1")))

        val result = cacheMemory.mget(keys)

        result shouldBe mapOf("k1" to "v1", "k2" to null)
        capturedKeys.captured.map(::decode) shouldBe keys
        verify(exactly = 1) { redisTemplate.execute(any<ReactiveRedisCallback<List<String?>>>()) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mset은 데이터가 비어있으면 저장을 건너뛴다`() = runTest {
        cacheMemory.mset(emptyMap())

        verify(exactly = 0) { redisTemplate.execute(any<ReactiveRedisCallback<Boolean>>()) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mset은 TTL이 없으면 네이티브 MSET으로 일괄 저장한다`() = runTest {
        mockExecuteWithConnection<Boolean>()
        val payload = linkedMapOf("k1" to 10, "k2" to 20)
        val capturedPayload = slot<Map<ByteBuffer, ByteBuffer>>()

        every { stringCommands.mSet(capture(capturedPayload)) } returns Mono.just(true)

        cacheMemory.mset(payload, null)

        capturedPayload.captured.entries.associate { (k, v) -> decode(k) to decode(v) } shouldBe
            mapOf("k1" to "10", "k2" to "20")
        verify(exactly = 1) { redisTemplate.execute(any<ReactiveRedisCallback<Boolean>>()) }
        verify(exactly = 0) { redisTemplate.opsForValue() }
    }

    @Test
    fun `mset은 TTL이 있으면 SET command stream으로 저장한다`() = runTest {
        mockExecuteWithConnection<Boolean>()
        val payload = linkedMapOf("k1" to 10, "k2" to 20)
        val commandPublisher = slot<Publisher<SetCommand>>()

        every { stringCommands.set(capture(commandPublisher)) } returns Flux.empty()

        cacheMemory.mset(payload, 60_000L)

        val commands = Flux.from(commandPublisher.captured).collectList().awaitSingle()

        commands.size shouldBe 2
        commands.map { decode(it.key) } shouldBe listOf("k1", "k2")
        commands.map { decode(it.value!!) } shouldBe listOf("10", "20")
        commands.forEach {
            it.expiration.orElseThrow().expirationTimeInMilliseconds shouldBe 60_000L
            it.option.orElseThrow() shouldBe SetOption.UPSERT
        }
        verify(exactly = 1) { redisTemplate.execute(any<ReactiveRedisCallback<Boolean>>()) }
    }

    @Test
    fun `decrBy는 increment 음수 연산을 사용한다`() = runTest {
        every { redisTemplate.opsForValue() } returns valueOps
        every { valueOps.increment("k1", -3L) } returns Mono.just(7L)

        val result = cacheMemory.decrBy("k1", 3L)

        result shouldBe 7L
        verify(exactly = 1) { valueOps.increment("k1", -3L) }
    }

    private inline fun <reified T> mockExecuteWithConnection() {
        every { redisTemplate.execute(any<ReactiveRedisCallback<T>>()) } answers {
            val callback = firstArg<ReactiveRedisCallback<T>>()
            Flux.from(callback.doInRedis(connection))
        }
    }

    private fun encode(value: String): ByteBuffer =
        StandardCharsets.UTF_8.encode(value)

    private fun decode(value: ByteBuffer): String =
        StandardCharsets.UTF_8.decode(value.asReadOnlyBuffer()).toString()
}
