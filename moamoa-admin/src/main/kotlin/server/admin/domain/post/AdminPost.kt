package server.admin.domain.post

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import server.admin.domain.techblog.AdminTechBlog
import support.admin.domain.BaseEntity

@Entity
@Table(name = "post")
data class AdminPost(
    @Column(name = "post_key", nullable = false, unique = false)
    val key: String,

    @Column(name = "title", nullable = false, unique = false)
    val title: String,

    @Column(name = "description", nullable = false, unique = false)
    val description: String,

    @Column(name = "thumbnail", nullable = false, unique = false)
    val thumbnail: String,

    @Column(name = "url", nullable = false, unique = false)
    val url: String,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tech_blog_id", nullable = false)
    val techBlog: AdminTechBlog
) : BaseEntity()
