package server.domain.post

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity
import java.time.LocalDateTime

@Table(name = "post")
data class Post(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("post_key")
    val key: String,

    @Column("title")
    val title: String,

    @Column("description")
    val description: String,

    @Column("thumbnail")
    val thumbnail: String,

    @Column("url")
    val url: String,

    @Column("published_at")
    val publishedAt: LocalDateTime,

    @Column("view_count")
    val viewCount: Long = 0,

    @Column("bookmark_count")
    val bookmarkCount: Long = 0,

    @Column("tech_blog_id")
    val techBlogId: Long
) : BaseEntity()