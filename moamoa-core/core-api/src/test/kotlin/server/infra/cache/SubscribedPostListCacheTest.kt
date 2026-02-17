package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.*
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.shared.cache.CacheMemory
import server.shared.cache.get
import server.fixture.createPostSummary
import test.UnitTest

class SubscribedPostListCacheTest : UnitTest() {
    @Test
    fun `버전이 없으면 기본 버전으로 구독 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscribedPostListCache(cacheMemory)
        val memberId = 3L
        val page = 1L
        val versionKey = "POST:LIST:SUBSCRIBED:$memberId:VER"
        val valueKey = "POST:LIST:SUBSCRIBED:$memberId:V:1:PAGE:$page:"
        val expected = listOf(createPostSummary())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns null
        coEvery { cacheMemory.get(valueKey, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) } returns expected

        val result = cache.get(memberId, page)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.get(valueKey, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) }
    }

    @Test
    fun `버전 키를 사용해 구독 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscribedPostListCache(cacheMemory)
        val memberId = 3L
        val page = 1L
        val versionKey = "POST:LIST:SUBSCRIBED:$memberId:VER"
        val valueKey = "POST:LIST:SUBSCRIBED:$memberId:V:5:PAGE:$page:"
        val posts = listOf(createPostSummary())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns 5L
        coEvery { cacheMemory.set(valueKey, posts, 60_000L) } just Runs

        cache.set(memberId, page, posts)

        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.set(valueKey, posts, 60_000L) }
    }

    @Test
    fun `버전을 증가시켜 구독 목록 캐시를 무효화한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = SubscribedPostListCache(cacheMemory)
        val memberId = 3L
        val versionKey = "POST:LIST:SUBSCRIBED:$memberId:VER"

        coEvery { cacheMemory.incr(versionKey) } returns 2L

        cache.evictAll(memberId)

        coVerify(exactly = 1) { cacheMemory.incr(versionKey) }
    }
}
