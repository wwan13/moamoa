package server.admin.feature.notice.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.admin.feature.notice.domain.AdminNoticeRepository
import server.admin.fixture.createAdminNotice
import test.UnitTest
import java.util.Optional

class AdminNoticeServiceTest : UnitTest() {

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
