package server.feature.postbookmark.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.feature.postbookmark.application.PostBookmarkService
import server.feature.postbookmark.application.PostBookmarkToggleCommand
import server.feature.postbookmark.application.PostBookmarkToggleResult
import server.feature.post.command.application.PostData
import server.security.Passport
import server.security.RequestPassport

@RestController
@RequestMapping("/api/post-bookmark")
class PostBookmarkController(
    private val postBookmarkService: PostBookmarkService
) {

    @PostMapping
    fun toggle(
        @RequestBody @Valid command: PostBookmarkToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<PostBookmarkToggleResult?> {
        val response = postBookmarkService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun bookmarkedPosts(
        @RequestPassport passport: Passport
    ): ResponseEntity<List<PostData>> {
        val response = postBookmarkService.bookmarkedPosts(passport.memberId)

        return ResponseEntity.ok(response)
    }
}
