package server.feature.category.application

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import server.feature.category.domain.Category
import test.UnitTest

class CategoryServiceTest : UnitTest() {
    @Test
    fun `카테고리 전체 목록을 반환한다`() {
        val service = CategoryService()

        val result = service.findAll()

        result shouldBe Category.validCategories.map(::CategoryData)
    }
}
