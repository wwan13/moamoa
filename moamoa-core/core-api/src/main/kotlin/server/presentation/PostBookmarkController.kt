package server.presentation

import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.application.PostBookmarkService
import server.application.PostBookmarkToggleCommand
import server.application.PostBookmarkToggleResult
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/post-bookmark")
class PostBookmarkController(
    private val postBookmarkService: PostBookmarkService
) {

    @PostMapping
    suspend fun toggle(
        @RequestBody command: PostBookmarkToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostBookmarkToggleResult?> {
        val request = postBookmarkService.toggle(command, passport.memberId)

        return ResponseEntity.ok(request)
    }
}