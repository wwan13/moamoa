package server.domain.techblog

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Table
import support.domain.BaseEntity

@Entity
@Table(name = "tech_blog")
data class TechBlog(
    @Column(name = "title", nullable = false, unique = true)
    var title: String,

    @Column(name = "tech_blog_key", nullable = false, unique = true)
    var key: String,

    @Column(name = "blog_url", nullable = false, unique = false)
    var blogUrl: String,

    @Column(name = "icon", nullable = false, unique = false)
    var icon: String
) : BaseEntity()
