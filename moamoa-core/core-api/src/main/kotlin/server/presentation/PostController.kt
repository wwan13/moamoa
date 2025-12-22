package server.presentation

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.PostService

@RestController
@RequestMapping("/api/post")
class PostController(
    private val postService: PostService
) {

    @GetMapping("/{postId}")
    suspend fun redirect(
        @PathVariable postId: Long
    ): ResponseEntity<Void> {
        val uri = postService.redirect(postId)

        return ResponseEntity
            .status(HttpStatus.MOVED_PERMANENTLY)
            .header(HttpHeaders.LOCATION, uri)
            .build()
    }
}