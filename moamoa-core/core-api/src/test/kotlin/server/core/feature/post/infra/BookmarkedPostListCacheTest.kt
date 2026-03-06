package server.core.feature.post.infra

import com.fasterxml.jackson.core.type.TypeReference
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.post.query.PostSummary
import server.core.feature.post.infra.BookmarkedPostListCache
import server.cache.CacheMemory
import server.core.fixture.createPostSummary
import server.core.support.domain.ListEntry
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
        val expected = ListEntry(
            count = 2L,
            list = listOf(createPostSummary())
        )

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns null
        coEvery { cacheMemory.get(valueKey, any<TypeReference<ListEntry<PostSummary>>>()) } returns expected

        val result = cache.get(memberId, page)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.get(valueKey, any<TypeReference<ListEntry<PostSummary>>>()) }
    }

    @Test
    fun `버전 키를 사용해 북마크 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = BookmarkedPostListCache(cacheMemory)
        val memberId = 1L
        val page = 2L
        val versionKey = "POST:LIST:BOOKMARKED:$memberId:VER"
        val valueKey = "POST:LIST:BOOKMARKED:$memberId:V:7:PAGE:$page:"
        val entry = ListEntry(
            count = 2L,
            list = listOf(createPostSummary())
        )

        coEvery { cacheMemory.get(versionKey, any<TypeReference<Long>>()) } returns 7L
        coEvery { cacheMemory.set(valueKey, entry, 60_000L) } just Runs

        cache.set(memberId, page, entry)

        coVerify(exactly = 1) { cacheMemory.get(versionKey, any<TypeReference<Long>>()) }
        coVerify(exactly = 1) { cacheMemory.set(valueKey, entry, 60_000L) }
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
