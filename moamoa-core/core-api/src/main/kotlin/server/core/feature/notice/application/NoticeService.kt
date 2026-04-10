package server.core.feature.notice.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.notice.domain.NoticeRepository

@Service
class NoticeService(
    private val noticeRepository: NoticeRepository,
) {

    @Transactional(readOnly = true)
    fun findById(noticeId: Long): NoticeData {
        return noticeRepository.findByIdOrNull(noticeId)?.let(::NoticeData)
            ?: throw NoSuchElementException("공지사항을 찾을 수 없습니다.")
    }
}
