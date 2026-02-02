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

class PostListCacheTest : UnitTest() {
    @Test
    fun `페이지와 사이즈로 게시글 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostListCache(cacheMemory)
        val key = "POST:LIST:PAGE:1:SIZE:20"
        val expected = listOf(createPostSummary())

        coEvery { cacheMemory.get(key, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) } returns expected

        val result = cache.get(1, 20)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) }
    }

    @Test
    fun `페이지와 사이즈로 게시글 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = PostListCache(cacheMemory)
        val key = "POST:LIST:PAGE:1:SIZE:20"
        val posts = listOf(createPostSummary())

        coEvery { cacheMemory.set(key, posts, 1_800_000L) } just Runs

        cache.set(1, 20, posts)

        coVerify(exactly = 1) { cacheMemory.set(key, posts, 1_800_000L) }
    }
}
