package server.admin.feature.notice.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.admin.feature.notice.domain.AdminNotice
import server.admin.feature.notice.domain.AdminNoticeRepository
import server.admin.fixture.createAdminNotice
import test.UnitTest
import java.util.Optional

class AdminNoticeServiceTest : UnitTest() {

    @Test
    fun `공지사항 생성 시 notice를 저장한다`() = runTest {
        val noticeRepository = mockk<AdminNoticeRepository>()
        val service = AdminNoticeService(noticeRepository)
        val command = AdminNoticeCreateCommand(
            title = "공지 제목",
            chip = "서비스안내",
            content = "<p>본문</p>",
            published = true,
        )
        val noticeSlot = slot<AdminNotice>()

        every { noticeRepository.save(capture(noticeSlot)) } answers { noticeSlot.captured }

        service.create(command)

        noticeSlot.captured.title shouldBe command.title
        noticeSlot.captured.chip shouldBe command.chip
        noticeSlot.captured.content shouldBe command.content
        noticeSlot.captured.published shouldBe command.published
    }

    @Test
    fun `상태 업데이트 시 공지사항의 published를 변경한다`() = runTest {
        val noticeRepository = mockk<AdminNoticeRepository>()
        val service = AdminNoticeService(noticeRepository)

        val noticeId = 10L
        val command = AdminUpdateNoticePublishedCommand(published = true)
        val notice = createAdminNotice(id = noticeId, published = false)

        every { noticeRepository.findById(noticeId) } returns Optional.of(notice)

        service.updatePublished(noticeId, command)

        notice.published shouldBe true
    }

    @Test
    fun `공지사항이 존재하지 않으면 예외가 발생한다`() = runTest {
        val noticeRepository = mockk<AdminNoticeRepository>()
        val service = AdminNoticeService(noticeRepository)

        val noticeId = 10L
        val command = AdminUpdateNoticePublishedCommand(published = true)

        every { noticeRepository.findById(noticeId) } returns Optional.empty()

        val exception = shouldThrow<NoSuchElementException> {
            service.updatePublished(noticeId, command)
        }

        exception.message shouldBe "존재하지 않는 공지사항 입니다."
        verify(exactly = 1) { noticeRepository.findById(noticeId) }
    }
}
