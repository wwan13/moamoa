package server.feature.postbookmark.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table("post_bookmark")
data class PostBookmark(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("member_id")
    val memberId: Long,

    @Column("post_id")
    val postId: Long
) : BaseEntity() {

    fun bookmark() = PostBookmarkUpdatedEvent(
        memberId = memberId,
        postId = postId,
        bookmarked = true
    )

    fun unbookmark() = PostBookmarkUpdatedEvent(
        memberId = memberId,
        postId = postId,
        bookmarked = false
    )
}