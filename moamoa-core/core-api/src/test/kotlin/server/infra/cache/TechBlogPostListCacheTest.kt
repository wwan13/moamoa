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

class TechBlogPostListCacheTest : UnitTest() {
    @Test
    fun `기술 블로그 게시글 목록을 조회한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogPostListCache(cacheMemory)
        val key = "POST:LIST:TECHBLOG:10:PAGE:1"
        val expected = listOf(createPostSummary())

        coEvery { cacheMemory.get(key, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) } returns expected

        val result = cache.get(10L, 1L)

        result shouldBe expected
        coVerify(exactly = 1) { cacheMemory.get(key, any<TypeReference<List<server.feature.post.query.PostSummary>>>()) }
    }

    @Test
    fun `기술 블로그 게시글 목록을 저장한다`() = runTest {
        val cacheMemory = mockk<CacheMemory>()
        val cache = TechBlogPostListCache(cacheMemory)
        val key = "POST:LIST:TECHBLOG:10:PAGE:1"
        val posts = listOf(createPostSummary())

        coEvery { cacheMemory.set(key, posts, 1_800_000L) } just Runs

        cache.set(10L, 1L, posts)

        coVerify(exactly = 1) { cacheMemory.set(key, posts, 1_800_000L) }
    }
}
