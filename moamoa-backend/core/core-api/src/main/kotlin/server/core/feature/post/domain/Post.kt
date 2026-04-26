package server.core.feature.post.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import server.core.support.domain.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(
    name = "post",
    uniqueConstraints = [UniqueConstraint(name = "uq_post_tech_blog_key", columnNames = ["tech_blog_id", "post_key"])]
)
class Post(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "post_key", length = 255, nullable = false)
    val key: String,

    @Column(name = "title", length = 255, nullable = false)
    val title: String,

    @Column(name = "description", length = 2047, nullable = false)
    val description: String,

    @Column(name = "thumbnail", length = 5000, nullable = false)
    val thumbnail: String,

    @Column(name = "url", length = 1027, nullable = false)
    val url: String,

    @Column(name = "published_at", nullable = false)
    val publishedAt: LocalDateTime,

    @Column(name = "view_count", nullable = false)
    val viewCount: Long = 0,

    bookmarkCount: Long = 0,

    @Column(name = "tech_blog_id", nullable = false)
    val techBlogId: Long,

    @Column(name = "category_id", nullable = false)
    val categoryId: Long,
) : BaseEntity() {
    @Column(name = "bookmark_count", nullable = false)
    var bookmarkCount: Long = bookmarkCount
        private set

    fun updateBookmarkCount(delta: Long) {
        bookmarkCount = (bookmarkCount + delta).coerceAtLeast(0)
    }
}
