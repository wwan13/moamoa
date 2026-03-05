package server.core.feature.bookmark.api

import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.post.application.PostData
import server.core.feature.bookmark.application.BookmarkService
import server.core.feature.bookmark.application.BookmarkToggleCommand
import server.core.feature.bookmark.application.BookmarkToggleResult
import server.core.global.security.Passport
import server.core.global.security.RequestPassport

@RestController
@RequestMapping("/api/bookmark")
class BookmarkController(
    private val bookmarkService: BookmarkService
) {

    @PostMapping
    fun toggle(
        @RequestBody @Valid command: BookmarkToggleCommand,
        @RequestPassport passport: Passport
    ): ResponseEntity<BookmarkToggleResult?> {
        val response = bookmarkService.toggle(command, passport.memberId)

        return ResponseEntity.ok(response)
    }

    @GetMapping
    fun bookmarkedPosts(
        @RequestPassport passport: Passport
    ): ResponseEntity<List<PostData>> {
        val response = bookmarkService.bookmarkedPosts(passport.memberId)

        return ResponseEntity.ok(response)
    }
}
