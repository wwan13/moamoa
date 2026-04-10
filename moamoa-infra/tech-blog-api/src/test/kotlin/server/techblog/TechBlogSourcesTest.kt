package server.techblog

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.mockk
import org.junit.jupiter.api.Test
import test.UnitTest

class TechBlogSourcesTest : UnitTest() {

    @Test
    fun `키 대소문자와 무관하게 source를 찾는다`() {
        val source = mockk<TechBlogSource>()
        val techBlogSources = TechBlogSources(
            mapOf("FooSource" to source)
        )

        techBlogSources["foo"] shouldBe source
        techBlogSources["FOO"] shouldBe source
    }

    @Test
    fun `존재하지 않는 키 조회 시 예외가 발생한다`() {
        val techBlogSources = TechBlogSources(emptyMap())

        val exception = shouldThrow<IllegalArgumentException> {
            techBlogSources["foo"]
        }

        exception.message shouldBe "존재하지 않는 tech blog 입니다."
    }

    @Test
    fun `존재 여부를 대소문자 무관하게 판단한다`() {
        val techBlogSources = TechBlogSources(
            mapOf("BarSource" to mockk())
        )

        techBlogSources.exists("bar") shouldBe true
        techBlogSources.exists("BAR") shouldBe true
        techBlogSources.exists("baz") shouldBe false
    }

    @Test
    fun `존재하지 않는 키 검증 시 예외가 발생한다`() {
        val techBlogSources = TechBlogSources(emptyMap())

        val exception = shouldThrow<IllegalArgumentException> {
            techBlogSources.validateExists("foo")
        }

        exception.message shouldBe "tech blog source가 존재하지 않습니다."
    }
}
