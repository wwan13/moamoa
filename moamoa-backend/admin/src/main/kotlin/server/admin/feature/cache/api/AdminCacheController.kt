package server.admin.feature.cache.api

import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.cache.application.AdminCacheEvictAllResult
import server.admin.feature.cache.application.AdminCacheEvictResult
import server.admin.feature.cache.application.AdminCacheService
import server.admin.feature.cache.application.AdminCacheSummary
import server.admin.global.security.AdminPassport
import server.admin.global.security.RequestAdminPassport
import server.admin.global.security.ensureAdmin
import server.admin.global.web.AdminApiResponse

@RestController
@RequestMapping("/api/admin/cache")
internal class AdminCacheController(
    private val adminCacheService: AdminCacheService,
) {

    @GetMapping
    fun findAll(
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<List<AdminCacheSummary>> {
        passport.ensureAdmin()
        return AdminApiResponse.of(adminCacheService.findAll())
    }

    @PostMapping("/{cacheKey}/evict")
    fun evict(
        @PathVariable cacheKey: String,
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<AdminCacheEvictResult> {
        passport.ensureAdmin()
        return AdminApiResponse.of(adminCacheService.evict(cacheKey))
    }

    @PostMapping("/evict-all")
    fun evictAll(
        @RequestAdminPassport passport: AdminPassport,
    ): AdminApiResponse<AdminCacheEvictAllResult> {
        passport.ensureAdmin()
        return AdminApiResponse.of(adminCacheService.evictAll())
    }
}
