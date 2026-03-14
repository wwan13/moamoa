package server.techblog

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest
import java.time.LocalDateTime

class TechBlogPostCatetorizerTest : UnitTest() {

    private val sut = TechBlogPostCatetorizer()

    @Test
    fun `ENGINEERING 키워드가 있으면 ENGINEERING을 반환한다`() {
        val post = sut.categorize(post("Spring Boot batch", "batch processing", listOf("backend")))

        post.category shouldBe TechBlogPostCategory.ENGINEERING
    }

    @Test
    fun `PRODUCT 키워드가 있으면 PRODUCT를 반환한다`() {
        val post = sut.categorize(post("Product discovery", "실험과 지표", listOf("product")))

        post.category shouldBe TechBlogPostCategory.PRODUCT
    }

    @Test
    fun `DESIGN 키워드가 있으면 DESIGN을 반환한다`() {
        val post = sut.categorize(post("Design system token", "UI/UX", listOf("design")))

        post.category shouldBe TechBlogPostCategory.DESIGN
    }

    @Test
    fun `ETC 키워드가 있으면 ETC를 반환한다`() {
        val post = sut.categorize(post("채용 인터뷰 프로세스 개선", "recruiting guide", listOf("채용")))

        post.category shouldBe TechBlogPostCategory.ETC
    }

    @Test
    fun `동점이면 UNDEFINED를 반환한다`() {
        val post = sut.categorize(post("kotlin and design", "", listOf("kotlin", "design")))

        post.category shouldBe TechBlogPostCategory.UNDEFINED
    }

    @Test
    fun `태그와 텍스트가 모두 비어 있으면 UNDEFINED를 반환한다`() {
        val post = sut.categorize(post("   ", " ", emptyList()))

        post.category shouldBe TechBlogPostCategory.UNDEFINED
    }

    private fun post(title: String, description: String, tags: List<String>): TechBlogPost =
        TechBlogPost(
            key = "test-key",
            title = title,
            description = description,
            tags = tags,
            thumbnail = "",
            publishedAt = LocalDateTime.of(2026, 1, 1, 0, 0),
            url = "https://example.com/post"
        )
}
