package server.core.feature.bookmark.domain

import org.springframework.data.jpa.repository.JpaRepository

interface BookmarkRepository : JpaRepository<Bookmark, Long> {
    fun findByMemberIdAndPostId(memberId: Long, postId: Long): Bookmark?

    fun findAllByMemberId(memberId: Long): List<Bookmark>

    fun countByMemberId(memberId: Long): Long

    fun deleteAllByMemberId(memberId: Long): Long
}
