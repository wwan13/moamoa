package server.admin.feature.post.command.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "post")
internal class AdminPost(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "post_key")
    val key: String,

    title: String,

    description: String,

    thumbnail: String,

    url: String,

    publishedAt: LocalDateTime,

    @Column(name = "tech_blog_id")
    val techBlogId: Long,

    categoryId: Long,
) : AdminBaseEntity() {
    @Column(name = "title")
    var title: String = title
        private set

    @Column(name = "description")
    var description: String = description
        private set

    @Column(name = "thumbnail")
    var thumbnail: String = thumbnail
        private set

    @Column(name = "url")
    var url: String = url
        private set

    @Column(name = "published_at")
    var publishedAt: LocalDateTime = publishedAt
        private set

    @Column(name = "category_id")
    var categoryId: Long = categoryId
        private set

    fun updateCollectedData(
        title: String,
        description: String,
        thumbnail: String,
        url: String,
        publishedAt: LocalDateTime,
    ) {
        this.title = title
        this.description = description
        this.thumbnail = thumbnail
        this.url = url
        this.publishedAt = publishedAt
    }

    fun updateCategory(categoryId: Long) {
        this.categoryId = categoryId
    }
}
