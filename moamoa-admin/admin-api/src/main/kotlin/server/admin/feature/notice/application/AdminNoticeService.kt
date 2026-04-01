package server.admin.feature.notice.application

import org.springframework.stereotype.Service
import server.admin.feature.notice.domain.AdminNoticeRepository

@Service
internal class AdminNoticeService(
    private val noticeRepository: AdminNoticeRepository
) {
}