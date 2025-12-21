package server.admin.domain.post

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.BaseEntity

@Table(name = "post")
data class AdminPost(
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

    @Column("tech_blog_id")
    val techBlogId: Long
) : BaseEntity()
