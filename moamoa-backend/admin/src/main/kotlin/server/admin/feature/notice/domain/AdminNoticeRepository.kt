package server.admin.feature.notice.domain

import org.springframework.data.jpa.repository.JpaRepository

internal interface AdminNoticeRepository : JpaRepository<AdminNotice, Long>
