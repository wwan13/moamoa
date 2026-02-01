package server.feature.postbookmark.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest

class PostBookmarkTest : UnitTest() {
    @Test
    fun `북마크를 추가하면 bookmarked는 true인 이벤트를 반환한다`() {
        val bookmark = PostBookmark(
            id = 1L,
            memberId = 10L,
            postId = 20L
        )

        val result = bookmark.bookmark()

        result shouldBe PostBookmarkUpdatedEvent(
            memberId = 10L,
            postId = 20L,
            bookmarked = true
        )
    }

    @Test
    fun `북마크를 해제하면 bookmarked는 false인 이벤트를 반환한다`() {
        val bookmark = PostBookmark(
            id = 1L,
            memberId = 10L,
            postId = 20L
        )

        val result = bookmark.unbookmark()

        result shouldBe PostBookmarkUpdatedEvent(
            memberId = 10L,
            postId = 20L,
            bookmarked = false
        )
    }
}
