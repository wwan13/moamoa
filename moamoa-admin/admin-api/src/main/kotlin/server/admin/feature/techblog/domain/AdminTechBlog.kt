package server.admin.feature.techblog.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.AdminBaseEntity

@Table(name = "tech_blog")
internal class AdminTechBlog(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("title")
    var title: String,

    @Column("tech_blog_key")
    val key: String,

    @Column("blog_url")
    var blogUrl: String,

    @Column("icon")
    var icon: String
) : AdminBaseEntity() {

    fun update(
        title: String,
        blogUrl: String,
        icon: String
    ) {
        this.title = title
        this.blogUrl = blogUrl
        this.icon = icon
    }
}
