package server.admin.feature.techblog.application

import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.admin.feature.post.command.domain.AdminPost
import server.admin.feature.post.command.domain.AdminPostCountProjection
import server.admin.feature.post.command.domain.AdminPostRepository
import server.admin.feature.posttag.domain.AdminPostTag
import server.admin.feature.posttag.domain.AdminPostTagRepository
import server.admin.feature.subscription.domain.AdminSubscriptionCountProjection
import server.admin.feature.subscription.domain.AdminSubscriptionRepository
import server.admin.feature.tag.domain.AdminTag
import server.admin.feature.tag.domain.AdminTagRepository
import server.admin.feature.techblog.domain.AdminTechBlog
import server.admin.feature.techblog.domain.AdminTechBlogRepository
import server.admin.feature.techblog.infra.TechBlogCollector
import server.techblog.TechBlogPost
import server.techblog.TechBlogPostCatetorizer

@Service
internal class AdminTechBlogService(
    private val techBlogRepository: AdminTechBlogRepository,
    private val postRepository: AdminPostRepository,
    private val subscriptionRepository: AdminSubscriptionRepository,
    private val techBlogCollector: TechBlogCollector,
    private val tagRepository: AdminTagRepository,
    private val postTagRepository: AdminPostTagRepository,
    private val techBlogCategorizer: TechBlogPostCatetorizer,
) {
    @Transactional(readOnly = true)
    fun findAll(): List<AdminTechBlogData> {
        val techBlogs = techBlogRepository.findAllByOrderByTitleAscIdAsc()
        if (techBlogs.isEmpty()) return emptyList()

        val techBlogIds = techBlogs.map { it.id }
        val postCounts = postRepository.countByTechBlogIds(techBlogIds)
            .associatePostCountsByTechBlogId()
        val subscriptionCounts = subscriptionRepository.countByTechBlogIds(techBlogIds)
            .associateSubscriptionCountsByTechBlogId()

        return techBlogs.map { techBlog ->
            AdminTechBlogData(
                techBlog = techBlog,
                postCount = postCounts[techBlog.id] ?: 0,
                subscriptionCount = subscriptionCounts[techBlog.id] ?: 0,
            )
        }
    }

    @Transactional
    fun create(command: AdminCreateTechBlogCommand): AdminTechBlogData {
        validateTitle(command.title)
        val techBlog = server.admin.feature.techblog.domain.AdminTechBlog(
            title = command.title,
            icon = command.icon,
            blogUrl = command.blogUrl,
            key = command.key,
        )
        techBlogCollector.validateExists(techBlog.key)
        return AdminTechBlogData(techBlogRepository.save(techBlog))
    }

    @Transactional
    fun update(id: Long, command: AdminUpdateTechBlogCommand): AdminTechBlogData {
        validateTitle(command.title)
        val techBlog = techBlogRepository.findByIdOrNull(id)
            ?: throw NoSuchElementException("존재하지 않는 tech blog 입니다.")
        techBlog.update(command.title, command.blogUrl, command.icon)
        return AdminTechBlogData(techBlog)
    }

    @Transactional
    fun deletePosts(techBlogId: Long): AdminDeleteTechBlogPostsResult {
        val techBlog = techBlogRepository.findByIdOrNull(techBlogId)
            ?: throw NoSuchElementException("존재하지 않는 tech blog 입니다.")
        val postIds = postRepository.findIdsByTechBlogId(techBlogId)

        if (postIds.isEmpty()) {
            return AdminDeleteTechBlogPostsResult(
                techBlog = AdminTechBlogData(techBlog),
                deletedPostCount = 0,
            )
        }

        postTagRepository.deleteAllByPostIdIn(postIds)
        val deletedPostCount = postRepository.deleteAllByTechBlogId(techBlogId)

        return AdminDeleteTechBlogPostsResult(
            techBlog = AdminTechBlogData(techBlog),
            deletedPostCount = deletedPostCount,
        )
    }

    private fun validateTitle(title: String) {
        if (techBlogRepository.existsByTitle(title)) {
            throw IllegalArgumentException("이미 존재하는 tech blog 입니다.")
        }
    }

    @Transactional
    fun createPosts(
        techBlogId: Long,
        fetchedPosts: List<TechBlogPost>,
    ): AdminCollectPostsResult {
        val techBlog = techBlogRepository.findByIdOrNull(techBlogId)
            ?: throw NoSuchElementException("존재하지 않는 tech blog 입니다.")
        val categorizedPosts = fetchedPosts.map(techBlogCategorizer::categorize)

        val tagsByTitle = upsertTags(categorizedPosts)
        val existingPostsByKey = findExistingPostsByKey(techBlog.id, categorizedPosts)
        val collectResult = saveCollectedPosts(techBlog, categorizedPosts, existingPostsByKey)
        syncPostTags(collectResult.posts, categorizedPosts, tagsByTitle)

        return AdminCollectPostsResult(
            techBlog = AdminTechBlogData(techBlog),
            newPostCount = collectResult.newPostCount,
            updatedPostCount = collectResult.updatedPostCount,
        )
    }

    private fun upsertTags(fetchedPosts: List<TechBlogPost>): Map<String, AdminTag> {
        val titles = fetchedPosts.flatMap { it.tags }.mapNotNull(::normalizeTagTitle).distinct()
        if (titles.isEmpty()) return emptyMap()

        val existing = tagRepository.findAllByTitleIn(titles)
        val existingTitles = existing.map { it.title.lowercase() }.toHashSet()
        val newTags = titles.asSequence()
            .filterNot { it in existingTitles }
            .map { AdminTag(title = it) }
            .toList()

        if (newTags.isEmpty()) return existing.associateBy { it.title }

        val saved = tagRepository.saveAll(newTags).toList()
        return (existing + saved).associateBy { it.title }
    }

    private fun findExistingPostsByKey(techBlogId: Long, fetchedPosts: List<TechBlogPost>): Map<String, AdminPost> {
        if (fetchedPosts.isEmpty()) return emptyMap()

        return postRepository.findAllByTechBlogIdAndKeyIn(techBlogId, fetchedPosts.map { it.key })
            .associateBy { it.key }
    }

    private fun saveCollectedPosts(
        techBlog: AdminTechBlog,
        fetchedPosts: List<TechBlogPost>,
        existingPostsByKey: Map<String, AdminPost>,
    ): CollectedPostsResult {
        fetchedPosts.forEach { fetchedPost ->
            existingPostsByKey[fetchedPost.key]?.updateCollectedData(
                title = fetchedPost.title,
                description = fetchedPost.description,
                thumbnail = fetchedPost.thumbnail,
                url = fetchedPost.url,
                publishedAt = fetchedPost.publishedAt,
            )
        }

        val newPosts = fetchedPosts.asSequence()
            .filter { it.key !in existingPostsByKey }
            .map {
                AdminPost(
                    key = it.key,
                    title = it.title,
                    description = it.description,
                    thumbnail = it.thumbnail,
                    url = it.url,
                    publishedAt = it.publishedAt,
                    techBlogId = techBlog.id,
                    categoryId = it.category.categoryId,
                )
            }
            .toList()

        val savedNewPosts = if (newPosts.isEmpty()) emptyList()
        else postRepository.saveAll(newPosts).toList()

        return CollectedPostsResult(
            posts = existingPostsByKey.values.toList() + savedNewPosts,
            newPostCount = savedNewPosts.size,
            updatedPostCount = existingPostsByKey.size,
        )
    }

    private fun syncPostTags(
        posts: List<AdminPost>,
        fetchedPosts: List<TechBlogPost>,
        tagsByTitle: Map<String, AdminTag>,
    ) {
        if (posts.isEmpty()) return

        val fetchedByKey = fetchedPosts.associateBy { it.key }
        val desiredPostTags = posts.flatMap { post ->
            val fetched = fetchedByKey[post.key]
                ?: throw IllegalStateException("포스트가 존재하지 않습니다.")

            fetched.tags
                .mapNotNull(::normalizeTagTitle)
                .map { normalizedTitle ->
                    val tag = tagsByTitle[normalizedTitle]
                        ?: throw IllegalStateException("태그가 존재하지 않습니다. title=$normalizedTitle")
                    post.id to tag.id
                }
        }

        val existingPostTags = postTagRepository.findAllByPostIdIn(posts.map { it.id })
        val existingPostTagKeys = existingPostTags.map { it.postId to it.tagId }.toHashSet()
        val desiredPostTagKeys = desiredPostTags.toHashSet()

        val postTagsToDelete = existingPostTags.filter { (it.postId to it.tagId) !in desiredPostTagKeys }
        if (postTagsToDelete.isNotEmpty()) {
            postTagRepository.deleteAll(postTagsToDelete)
        }

        val postTagsToAdd = desiredPostTagKeys.asSequence()
            .filterNot { it in existingPostTagKeys }
            .map { (postId, tagId) -> AdminPostTag(postId = postId, tagId = tagId) }
            .toList()

        if (postTagsToAdd.isNotEmpty()) {
            postTagRepository.saveAll(postTagsToAdd).toList()
        }
    }

    private fun normalizeTagTitle(title: String): String? {
        val normalized = title.trim().lowercase()
        return normalized.takeIf { it.isNotBlank() }
    }

    private data class CollectedPostsResult(
        val posts: List<AdminPost>,
        val newPostCount: Int,
        val updatedPostCount: Int,
    )
}

private fun List<AdminPostCountProjection>.associatePostCountsByTechBlogId(): Map<Long, Long> {
    return associate { it.techBlogId to it.count }
}

private fun List<AdminSubscriptionCountProjection>.associateSubscriptionCountsByTechBlogId(): Map<Long, Long> {
    return associate { it.techBlogId to it.count }
}
