package server.feature.postbookmark.domain

import org.springframework.data.jpa.repository.JpaRepository

interface PostBookmarkRepository : JpaRepository<PostBookmark, Long> {
    fun findByMemberIdAndPostId(memberId: Long, postId: Long): PostBookmark?

    fun findAllByMemberId(memberId: Long): List<PostBookmark>

    fun countByMemberId(memberId: Long): Long

    fun deleteAllByMemberId(memberId: Long): Long
}
