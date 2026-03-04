package server.admin.feature.post.command.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import support.admin.domain.AdminBaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "post")
internal class AdminPost(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "post_key")
    val key: String,

    @Column(name = "title")
    val title: String,

    @Column(name = "description")
    val description: String,

    @Column(name = "thumbnail")
    val thumbnail: String,

    @Column(name = "url")
    val url: String,

    @Column(name = "published_at")
    val publishedAt: LocalDateTime,

    @Column(name = "tech_blog_id")
    val techBlogId: Long,

    categoryId: Long,
) : AdminBaseEntity() {
    @Column(name = "category_id")
    var categoryId: Long = categoryId
        private set


    fun updateCategory(categoryId: Long) {
        this.categoryId = categoryId
    }
}
