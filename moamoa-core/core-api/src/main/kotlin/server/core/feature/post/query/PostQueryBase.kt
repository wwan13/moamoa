package server.core.feature.post.query

import com.linecorp.kotlinjdsl.dsl.jpql.Jpql
import server.core.feature.post.domain.Post
import server.core.feature.techblog.domain.TechBlog

internal fun Jpql.selectBasePostSummary(isBookmarked: Boolean) = selectNew<PostSummary>(
    path(Post::id),
    path(Post::key),
    path(Post::title),
    path(Post::description),
    path(Post::thumbnail),
    path(Post::url),
    path(Post::publishedAt),
    booleanLiteral(isBookmarked),
    path(Post::viewCount),
    path(Post::bookmarkCount),
    path(TechBlog::id),
    path(TechBlog::title),
    path(TechBlog::icon),
    path(TechBlog::blogUrl),
    path(TechBlog::key),
    path(TechBlog::subscriptionCount),
)
