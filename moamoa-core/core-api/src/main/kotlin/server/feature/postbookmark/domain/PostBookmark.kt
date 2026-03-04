package server.feature.postbookmark.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import support.domain.BaseEntity

@Entity
@Table(
    name = "post_bookmark",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_post", columnNames = ["member_id", "post_id"])],
    indexes = [Index(name = "idx_post_id", columnList = "post_id")]
)
class PostBookmark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "post_id", nullable = false)
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
