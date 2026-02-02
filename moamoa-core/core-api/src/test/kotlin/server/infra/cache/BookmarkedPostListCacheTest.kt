package server.infra.cache

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.cache.CacheMemory
import server.fixture.createPostSummary
import test.UnitTest

class BookmarkedPostListCacheTest : UnitTest() {
    @Test
    fun `버전이 없으면 기본 버전으로 북마크 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedPostListCache(cacheMemory)
        val memberId = 1L
        val page = 2L
        val versionKey = "POST:LIST:BOOKMARKED:$memberId:VER"
        val valueKey = "POST:LIST:BOOKMARKED:$memberId:V:1:PAGE:$page:"
        val expected = listOf(createPostSummary())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns null
        coEvery { cacheMemory.get(valueKey, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) } returns expected

        val result = cache.get(memberId, page)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.get(valueKey, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) }
    }

    @Test
    fun `버전 키를 사용해 북마크 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedPostListCache(cacheMemory)
        val memberId = 1L
        val page = 2L
        val versionKey = "POST:LIST:BOOKMARKED:$memberId:VER"
        val valueKey = "POST:LIST:BOOKMARKED:$memberId:V:7:PAGE:$page:"
        val posts = listOf(createPostSummary())

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns 7L
        coEvery { cacheMemory.set(valueKey, posts, 60_000L) } just Runs

        cache.set(memberId, page, posts)

        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.set(valueKey, posts, 60_000L) }
    }

    @Test
    fun `버전을 증가시켜 북마크 목록 캐시를 무효화한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedPostListCache(cacheMemory)
        val memberId = 1L
        val versionKey = "POST:LIST:BOOKMARKED:$memberId:VER"

        coEvery { cacheMemory.incr(versionKey) } returns 2L

        cache.evictAll(memberId)

        coVerify(exactly = 1) { cacheMemory.incr(versionKey) }
    }
}
