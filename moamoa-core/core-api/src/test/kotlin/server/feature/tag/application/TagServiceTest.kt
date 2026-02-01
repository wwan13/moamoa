package server.feature.tag.application

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.tag.domain.Tag
import server.feature.tag.domain.TagRepository
import test.UnitTest

class TagServiceTest : UnitTest() {
    @Test
    fun `태그 목록을 제목 오름차순으로 조회해 TagData로 변환한다`() = runTest {
        val tagRepository = mockk<TagRepository>()
        val service = TagService(tagRepository)

        val tags = listOf(
            Tag(id = 2L, title = "Backend"),
            Tag(id = 1L, title = "Android")
        )
        coEvery { tagRepository.findAllByOrderByTitleAsc() } returns tags

        val result = service.findAll()

        result shouldBe listOf(
            TagData(id = 2L, title = "Backend"),
            TagData(id = 1L, title = "Android")
        )
        coVerify(exactly = 1) { tagRepository.findAllByOrderByTitleAsc() }
    }

    @Test
    fun `태그가 없으면 빈 목록을 반환한다`() = runTest {
        val tagRepository = mockk<TagRepository>()
        val service = TagService(tagRepository)

        coEvery { tagRepository.findAllByOrderByTitleAsc() } returns emptyList()

        val result = service.findAll()

        result shouldBe emptyList()
        coVerify(exactly = 1) { tagRepository.findAllByOrderByTitleAsc() }
    }
}
