package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.cache.CacheMemory
import server.shared.cache.get
import server.shared.cache.mgetAs
import server.feature.post.query.PostStats
import server.fixture.createPostStats
import test.UnitTest

class PostStatsCacheTest : UnitTest() {
    @Test
    fun `게시글 통계를 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)
        val key = "POST:STATS:10"
        val expected = createPostStats(postId = 10L, viewCount = 5, bookmarkCount = 2)

        coEvery { cacheMemory.get(key, any<TypeReference<PostStats>>()) } returns expected

        val result = cache.get(10L)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<PostStats>>()) }
    }

    @Test
    fun `id 목록이 비어있으면 빈 맵을 반환한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)

        val result = cache.mGet(emptyList())

        result shouldBe emptyMap()
        coVerify(exactly = 0) { cacheMemory.mgetAs(any(), any<TypeReference<PostStats>>()) }
    }

    @Test
    fun `id 목록으로 게시글 통계를 일괄 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)
        val key1 = "POST:STATS:1"
        val key2 = "POST:STATS:2"
        val stats1 = createPostStats(postId = 1L, viewCount = 10, bookmarkCount = 3)

        coEvery { cacheMemory.mgetAs(setOf(key1, key2), any<TypeReference<PostStats>>()) } returns mapOf(
            key1 to stats1,
            key2 to null
        )

        val result = cache.mGet(listOf(1L, 1L, 2L))

        result shouldBe mapOf(1L to stats1, 2L to null)
        coVerify(exactly = 1) { cacheMemory.mgetAs(setOf(key1, key2), any<TypeReference<PostStats>>()) }
    }

    @Test
    fun `게시글 통계를 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)
        val key = "POST:STATS:10"
        val stats = createPostStats(postId = 10L)

        coEvery { cacheMemory.set(key, stats, 60_000L) } just Runs

        cache.set(10L, stats)

        coVerify(exactly = 1) { cacheMemory.set(key, stats, 60_000L) }
    }

    @Test
    fun `통계 데이터가 비어있으면 일괄 저장을 건너뛴다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)

        cache.mSet(emptyMap())

        coVerify(exactly = 0) { cacheMemory.mset(any(), any()) }
    }

    @Test
    fun `게시글 통계를 일괄 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)
        val stats1 = createPostStats(postId = 1L, viewCount = 10, bookmarkCount = 3)
        val stats2 = createPostStats(postId = 2L, viewCount = 5, bookmarkCount = 1)
        val payload = mapOf(
            "POST:STATS:1" to stats1,
            "POST:STATS:2" to stats2
        )

        coEvery { cacheMemory.mset(payload, 60_000L) } just Runs

        cache.mSet(mapOf(1L to stats1, 2L to stats2))

        coVerify(exactly = 1) { cacheMemory.mset(payload, 60_000L) }
    }

    @Test
    fun `게시글 통계를 삭제한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostStatsCache(cacheMemory)
        val key = "POST:STATS:10"

        coEvery { cacheMemory.evict(key) } just Runs

        cache.evict(10L)

        coVerify(exactly = 1) { cacheMemory.evict(key) }
    }
}
