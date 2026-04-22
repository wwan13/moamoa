package server.admin.feature.notice.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.notice.application.AdminNoticeService
import server.admin.feature.notice.application.AdminUpdateNoticePublishedCommand
import server.admin.feature.notice.query.AdminNoticeList
import server.admin.feature.notice.query.AdminNoticeQueryConditions
import server.admin.feature.notice.query.AdminNoticeQueryService
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport
import server.admin.global.security.ensureAdmin
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/notice")
internal class AdminNoticeController(
    private val noticeService: AdminNoticeService,
    private val noticeQueryService: AdminNoticeQueryService,
) {

    @GetMapping
    fun findByConditions(
        conditions: AdminNoticeQueryConditions,
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<AdminNoticeList> {
        passport.ensureAdmin()
        val response = noticeQueryService.findByConditions(conditions)
        return AdminApiResponse.of(response)
    }

    @PostMapping("/published/{noticeId}")
    fun updatePublished(
        @PathVariable noticeId: Long,
        @RequestBody @Valid command: AdminUpdateNoticePublishedCommand,
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<Unit> {
        passport.ensureAdmin()
        noticeService.updatePublished(noticeId, command)
        return AdminApiResponse.of()
    }
}
