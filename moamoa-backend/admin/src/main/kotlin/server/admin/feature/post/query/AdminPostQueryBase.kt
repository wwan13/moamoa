package server.admin.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import server.admin.feature.post.command.domain.AdminPost
import server.admin.feature.techblog.domain.AdminTechBlog

internal fun Jpql.selectBaseAdminPostRow() = selectNew<AdminPostRow>(
    path(AdminPost::id),
    path(AdminPost::key),
    path(AdminPost::title),
    path(AdminPost::description),
    path(AdminPost::thumbnail),
    path(AdminPost::url),
    path(AdminPost::publishedAt),
    path(AdminPost::categoryId),
    path(AdminTechBlog::id),
    path(AdminTechBlog::title),
    path(AdminTechBlog::icon),
    path(AdminTechBlog::blogUrl),
    path(AdminTechBlog::key),
)
