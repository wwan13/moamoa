package server.admin.feature.notice.application

import org.springframework.stereotype.Service
import org.springframework.data.repository.findByIdOrNull
import org.springframework.transaction.annotation.Transactional
import server.admin.feature.notice.domain.AdminNotice
import server.admin.feature.notice.domain.AdminNoticeRepository
import java.time.LocalDateTime

@Service
internal class AdminNoticeService(
    private val noticeRepository: AdminNoticeRepository
) {
    @Transactional
    fun create(command: AdminNoticeCreateCommand) {
        noticeRepository.save(
            AdminNotice(
                title = command.title,
                chip = command.chip,
                content = command.content,
                published = command.published,
                publishedAt = LocalDateTime.now(),
            ),
        )
    }

    @Transactional
    fun updatePublished(
        noticeId: Long,
        command: AdminUpdateNoticePublishedCommand,
    ) {
        val notice = noticeRepository.findByIdOrNull(noticeId)
            ?: throw NoSuchElementException("존재하지 않는 공지사항 입니다.")

        notice.updatePublished(command.published)
    }
}
