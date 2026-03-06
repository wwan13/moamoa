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
import server.core.feature.post.infra.PostListCache
import server.cache.CacheMemory
import server.core.fixture.createPostSummary
import server.core.support.domain.ListEntry
import test.UnitTest

class PostListCacheTest : UnitTest() {
    @Test
    fun `페이지와 사이즈로 게시글 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostListCache(cacheMemory)
        val key = "POST:LIST:PAGE:1:SIZE:20"
        val expected = ListEntry(
            count = 3L,
            list = listOf(createPostSummary())
        )

        coEvery { cacheMemory.get(key, any<TypeReference<ListEntry<PostSummary>>>()) } returns expected

        val result = cache.get(1, 20)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<ListEntry<PostSummary>>>()) }
    }

    @Test
    fun `페이지와 사이즈로 게시글 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostListCache(cacheMemory)
        val key = "POST:LIST:PAGE:1:SIZE:20"
        val entry = ListEntry(
            count = 3L,
            list = listOf(createPostSummary())
        )

        coEvery { cacheMemory.set(key, entry, 1_800_000L) } just Runs

        cache.set(1, 20, entry)

        coVerify(exactly = 1) { cacheMemory.set(key, entry, 1_800_000L) }
    }
}
