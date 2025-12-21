package server.admin.domain.techblog

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.admin.domain.BaseEntity

@Table(name = "tech_blog")
class AdminTechBlog(
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
) : BaseEntity() {

    val clientKey: String
        get() = key + CLIENT_BEAN_SUFFIX

    fun update(
        title: String,
        blogUrl: String,
        icon: String
    ) {
        this.title = title
        this.blogUrl = blogUrl
        this.icon = icon
    }

    companion object {
        const val CLIENT_BEAN_SUFFIX = "Client"
    }
}
