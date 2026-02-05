package server.admin.feature.post.api

import kotlinx.coroutines.flow.toList
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.admin.feature.post.command.application.AdminPostService
import server.admin.feature.post.query.AdminPostQueryConditions
import server.admin.feature.post.query.AdminPostQueryService
import server.admin.feature.post.query.AdminPostSummary

@RestController
@RequestMapping("/api/admin/post")
internal class AdminPostController(
    private val postService: AdminPostService,
    private val postQueryService: AdminPostQueryService
) {

    @GetMapping
    suspend fun findByConditions(
        conditions: AdminPostQueryConditions
    ): ResponseEntity<List<AdminPostSummary>> {
        val response = postQueryService.findByConditions(conditions).toList()

        return ResponseEntity.ok(response)
    }
}
