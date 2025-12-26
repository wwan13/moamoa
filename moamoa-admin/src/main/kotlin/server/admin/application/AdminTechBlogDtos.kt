package server.admin.application

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import org.hibernate.validator.constraints.URL
import server.admin.domain.techblog.AdminTechBlog

data class AdminCreateTechBlogCommand(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    @field:Size(max = 50)
    val key: String,

    @field:NotBlank
    @field:URL
    val icon: String,

    @field:NotBlank
    @field:URL
    val blogUrl: String
)

data class AdminUpdateTechBlogCommand(
    @field:NotBlank
    @field:Size(max = 100)
    val title: String,

    @field:NotBlank
    @field:URL
    val icon: String,

    @field:NotBlank
    @field:URL
    val blogUrl: String
)

data class AdminTechBlogData(
    val id: Long,
    val title: String,
    val icon: String,
    val blogUrl: String,
    val key: String
) {
    constructor(
        techBlog: AdminTechBlog
    ) : this(
        id = techBlog.id,
        title = techBlog.title,
        key = techBlog.key,
        icon = techBlog.icon,
        blogUrl = techBlog.blogUrl
    )
}