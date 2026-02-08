package server.batch.post.processor

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContain
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Test
import server.batch.post.dto.PostSummary
import test.UnitTest
import java.io.InputStream

class PreCategorizingPostProcessorTest : UnitTest() {

    private val objectMapper = jacksonObjectMapper()
    private val sut = PreCategorizingPostProcessor(objectMapper)

    @Test
    fun `ENGINEERING 키워드가 있으면 10으로 분류한다`() {
        val item = summary(
            postId = 1L,
            title = "Spring Boot batch",
            description = "batch processing",
            tags = listOf("backend")
        )

        val result = sut.process(listOf(item))

        result.categorized.shouldContainExactly(
            server.batch.post.dto.PostCategory(postId = 1L, categoryId = 10L)
        )
        result.uncategorized shouldBe emptyList()
    }

    @Test
    fun `PRODUCT 키워드가 있으면 20으로 분류한다`() {
        val item = summary(
            postId = 2L,
            title = "Product discovery",
            description = "실험과 지표",
            tags = listOf("product")
        )

        val result = sut.process(listOf(item))

        result.categorized.shouldContainExactly(
            server.batch.post.dto.PostCategory(postId = 2L, categoryId = 20L)
        )
        result.uncategorized shouldBe emptyList()
    }

    @Test
    fun `DESIGN 키워드가 있으면 30으로 분류한다`() {
        val item = summary(
            postId = 3L,
            title = "Design system token",
            description = "UI/UX",
            tags = listOf("design")
        )

        val result = sut.process(listOf(item))

        result.categorized.shouldContainExactly(
            server.batch.post.dto.PostCategory(postId = 3L, categoryId = 30L)
        )
        result.uncategorized shouldBe emptyList()
    }

    @Test
    fun `동점이면 uncategorized로 분류한다`() {
        val item = summary(
            postId = 4L,
            title = "kotlin and design",
            description = "",
            tags = listOf("kotlin", "design")
        )

        val result = sut.process(listOf(item))

        result.categorized shouldBe emptyList()
        result.uncategorized.shouldContain(item)
    }

    @Test
    fun `태그와 텍스트가 모두 비어 있으면 uncategorized로 분류한다`() {
        val item = summary(
            postId = 5L,
            title = "   ",
            description = " ",
            tags = emptyList()
        )

        val result = sut.process(listOf(item))

        result.categorized shouldBe emptyList()
        result.uncategorized.shouldContain(item)
    }

    @Test
    fun `필수 카테고리 키가 누락되면 예외가 발생한다`() {
        val mapper = mockk<ObjectMapper>()
        every {
            mapper.readValue(any<InputStream>(), any<TypeReference<Map<String, List<String>>>>())
        } returns mapOf(
            "ENGINEERING" to listOf("backend"),
            "PRODUCT" to listOf("product")
        )

        shouldThrow<IllegalStateException> {
            PreCategorizingPostProcessor(mapper)
        }
    }

    private fun summary(
        postId: Long,
        title: String,
        description: String,
        tags: List<String>
    ): PostSummary =
        PostSummary(
            postId = postId,
            title = title,
            description = description,
            key = "key-$postId",
            tags = tags
        )
}
