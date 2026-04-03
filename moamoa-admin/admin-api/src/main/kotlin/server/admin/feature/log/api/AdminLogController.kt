package server.admin.feature.log.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.log.query.AdminLogPage
import server.admin.feature.log.query.AdminLogQueryConditions
import server.admin.feature.log.query.AdminLogQueryService
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport
import server.admin.global.security.ensureAdmin
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/log")
internal class AdminLogController(
    private val logQueryService: AdminLogQueryService,
) {

    @GetMapping
    fun findByConditions(
        conditions: AdminLogQueryConditions,
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<AdminLogPage> {
        passport.ensureAdmin()
        val response = logQueryService.findByConditions(conditions)
        return AdminApiResponse.of(response)
    }
}
