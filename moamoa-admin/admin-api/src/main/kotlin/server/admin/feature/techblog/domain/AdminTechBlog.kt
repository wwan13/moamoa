package server.admin.feature.techblog.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.AdminBaseEntity

@Table(name = "tech_blog")
internal data class AdminTechBlog(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("title")
    val title: String,

    @Column("tech_blog_key")
    val key: String,

    @Column("blog_url")
    val blogUrl: String,

    @Column("icon")
    val icon: String
) : AdminBaseEntity() {

    fun update(
        title: String,
        blogUrl: String,
        icon: String
    ): AdminTechBlog = copy(
        title = title,
        blogUrl = blogUrl,
        icon = icon
    )
}
