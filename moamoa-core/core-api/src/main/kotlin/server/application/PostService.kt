package server.application

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.application.query.PostQueryRepository
import server.infra.cache.PostViewCountCache
import server.security.Passport
import support.paging.Paging
import support.paging.calculateTotalPage

@Service
class PostService(
    private val postQueryRepository: PostQueryRepository,
    private val postViewCountCache: PostViewCountCache
) {

    suspend fun increaseViewCount(postId: Long): IncreaseViewCountResult {
        postViewCountCache.incr(postId)
        return IncreaseViewCountResult(true)
    }

    suspend fun findByConditions(
        conditions: PostQueryConditions,
        passport: Passport?
    ): PostList {
        val paging = Paging(
            size = conditions.size ?: 20,
            page = conditions.page ?: 1
        )

        val totalCount = postQueryRepository.countByConditions()
        val meta = ListMeta(
            page = paging.page,
            size = paging.size,
            totalCount = totalCount,
            totalPages = calculateTotalPage(totalCount, paging.size)
        )

        val posts = postQueryRepository.findAllByConditions(paging, passport?.memberId).toList()

        return PostList(meta, posts)
    }
}