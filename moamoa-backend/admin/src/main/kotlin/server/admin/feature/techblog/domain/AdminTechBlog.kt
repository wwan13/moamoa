package server.admin.feature.techblog.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "tech_blog")
internal class AdminTechBlog(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    title: String,

    @Column(name = "tech_blog_key")
    val key: String,

    blogUrl: String,

    icon: String,
) : AdminBaseEntity() {
    @Column(name = "title")
    var title: String = title
        private set

    @Column(name = "blog_url")
    var blogUrl: String = blogUrl
        private set

    @Column(name = "icon")
    var icon: String = icon
        private set


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
