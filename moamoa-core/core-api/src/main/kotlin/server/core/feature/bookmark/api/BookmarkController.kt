package server.core.feature.bookmark.api

import jakarta.validation.Valid
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import server.core.feature.post.application.PostData
import server.core.feature.bookmark.application.BookmarkCommand
import server.core.feature.bookmark.application.BookmarkService
import server.core.global.security.Passport
import server.core.global.security.RequestPassport
import server.core.global.web.ApiResponse

@RestController
@RequestMapping("/api/bookmark")
class BookmarkController(
    private val bookmarkService: BookmarkService
) {

    @PostMapping
    fun bookmark(
        @RequestBody @Valid command: BookmarkCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        bookmarkService.bookmark(command, passport.memberId)

        return ApiResponse.of()
    }

    @DeleteMapping
    fun unbookmark(
        @RequestBody @Valid command: BookmarkCommand,
        @RequestPassport passport: Passport
    ): ApiResponse<Unit> {
        bookmarkService.unbookmark(command, passport.memberId)

        return ApiResponse.of()
    }

    @GetMapping
    fun bookmarkedPosts(
        @RequestPassport passport: Passport
    ): ApiResponse<List<PostData>> {
        val response = bookmarkService.bookmarkedPosts(passport.memberId)

        return ApiResponse.of(response)
    }
}
