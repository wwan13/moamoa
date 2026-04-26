package server.core.feature.bookmark.infra

import org.springframework.stereotype.Component
import server.lock.KeyedLock

@Component
class BookmarkLock(
    private val keyedLock: KeyedLock
) {

    fun withLock(
        memberId: Long,
        postId: Long,
        block: () -> Unit,
    ) {
        val key = "bookmark:$memberId:${postId}"
        keyedLock.withLock(key) { block() }
    }
}