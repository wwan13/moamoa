package server.core.feature.bookmark.domain

import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test
import test.UnitTest

class BookmarkTest : UnitTest() {
    @Test
    fun `북마크 생성 시 입력값을 보관한다`() {
        val bookmark = Bookmark(
            id = 1L,
            memberId = 10L,
            postId = 20L
        )

        bookmark.id shouldBe 1L
        bookmark.memberId shouldBe 10L
        bookmark.postId shouldBe 20L
    }
}
