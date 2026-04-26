package server.core.feature.techblog.query

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import server.core.feature.techblog.domain.TechBlog

internal fun Jpql.selectBaseTechBlogSummary() = selectNew<TechBlogSummary>(
    path(TechBlog::id),
    path(TechBlog::title),
    path(TechBlog::icon),
    path(TechBlog::blogUrl),
    path(TechBlog::key),
    path(TechBlog::subscriptionCount),
    longLiteral(0L),
    booleanLiteral(false),
    booleanLiteral(false),
)
