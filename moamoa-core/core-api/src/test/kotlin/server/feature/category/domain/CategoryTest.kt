package server.feature.category.domain

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest

class CategoryTest : UnitTest() {
    @Test
    fun `유효 카테고리 목록은 UNDEFINED를 제외한다`() {
        val result = Category.validCategories

        result.contains(Category.UNDEFINED) shouldBe false
    }

    @Test
    fun `id로 카테고리를 조회한다`() {
        val result = Category.fromId(Category.BACKEND.id)

        result shouldBe Category.BACKEND
    }

    @Test
    fun `존재하지 않는 id면 예외가 발생한다`() {
        shouldThrow<IllegalArgumentException> {
            Category.fromId(9999L)
        }
    }

    @Test
    fun `name으로 카테고리를 조회한다`() {
        val result = Category.fromName(Category.FRONTEND.name)

        result shouldBe Category.FRONTEND
    }

    @Test
    fun `존재하지 않는 name이면 예외가 발생한다`() {
        shouldThrow<IllegalArgumentException> {
            Category.fromName("INVALID")
        }
    }
}
