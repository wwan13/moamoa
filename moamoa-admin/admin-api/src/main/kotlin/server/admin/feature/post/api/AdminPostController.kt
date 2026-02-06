package server.admin.feature.post.api

import jakarta.validation.Valid
import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.post.command.application.AdminPostService
import server.admin.feature.post.command.application.AdminUpdateCategoryCommand
import server.admin.feature.post.command.application.AdminUpdateCategoryResult
import server.admin.feature.post.query.AdminPostQueryConditions
import server.admin.feature.post.query.AdminPostQueryService
import server.admin.feature.post.query.AdminPostSummary
import server.admin.security.AdminPassport
import server.admin.security.RequestAdminPassport
import server.admin.security.ensureAdmin

@RestController
@RequestMapping("/api/admin/post")
internal class AdminPostController(
    private val postService: AdminPostService,
    private val postQueryService: AdminPostQueryService
) {

    @GetMapping
    suspend fun findByConditions(
        conditions: AdminPostQueryConditions,
        @RequestAdminPassport passport: AdminPassport
    ): ResponseEntity<List<AdminPostSummary>> {
        passport.ensureAdmin()
        val response = postQueryService.findByConditions(conditions).toList()
        return ResponseEntity.ok(response)
    }

    @PatchMapping("/{postId}")
    suspend fun updateCategory(
        @PathVariable postId: Long,
        @RequestBody @Valid command: AdminUpdateCategoryCommand,
        @RequestAdminPassport passport: AdminPassport
    ): ResponseEntity<AdminUpdateCategoryResult> {
        passport.ensureAdmin()
        val response = postService.updateCategory(postId, command)
        return ResponseEntity.ok(response)
    }
}
