package server.admin.domain.techblog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import support.admin.domain.BaseEntity

@Entity
@Table(
    name = "tech_blog"
)
class AdminTechBlog(
    @Column(name = "title", nullable = false, unique = true)
    var title: String,

    @Column(name = "tech_blog_key", nullable = false, unique = true)
    val key: String,

    @Column(name = "blog_url", nullable = false, unique = false)
    var blogUrl: String,

    @Column(name = "icon", nullable = false, unique = false)
    var icon: String
) : BaseEntity() {

    val clientBeanName: String
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
