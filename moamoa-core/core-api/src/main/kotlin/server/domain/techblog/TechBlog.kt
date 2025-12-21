package server.domain.techblog

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table(name = "tech_blog")
data class TechBlog(
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
) : BaseEntity()
