package server.admin.application

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.admin.infra.db.AdminTransactional
import server.admin.domain.post.AdminPost
import server.admin.domain.category.AdminCategory
import server.admin.domain.post.AdminPostRepository
import server.admin.domain.posttag.AdminPostTag
import server.admin.domain.posttag.AdminPostTagRepository
import server.admin.domain.tag.AdminTag
import server.admin.domain.tag.AdminTagRepository
import server.admin.domain.techblog.AdminTechBlog
import server.admin.domain.techblog.AdminTechBlogRepository
import server.techblog.TechBlogPost
import server.techblog.TechBlogSources

@Service
internal class AdminPostService(
    private val transactional: AdminTransactional,
    private val postRepository: AdminPostRepository,
    private val techBlogRepository: AdminTechBlogRepository,
    private val tagRepository: AdminTagRepository,
    private val postTagRepository: AdminPostTagRepository,
    private val techBlogSources: TechBlogSources
) {
    suspend fun initPosts(command: AdminInitPostsCommand): AdminInitPostsResult {
        if (postRepository.existsByTechBlogId(command.techBlogId)) {
            throw IllegalArgumentException("이미 초기화된 tech blog 입니다.")
        }

        val techBlog = techBlogRepository.findById(command.techBlogId)
            ?: throw IllegalArgumentException("존재하지 않는 tech blog 입니다.")

        val techBlogClient = techBlogSources[techBlog.key]
        val fetchedPosts = techBlogClient.getPosts().toList()

        return transactional {
            val categoriesByTitle = upsertTags(fetchedPosts)
            val savedPosts = savePosts(techBlog, fetchedPosts)
            savePostTags(savedPosts, fetchedPosts, categoriesByTitle)

            AdminInitPostsResult(
                techBlog = AdminTechBlogData(techBlog),
                newPostCount = savedPosts.size,
            )
        }
    }

    private suspend  fun upsertTags(fetchedPosts: List<TechBlogPost>): Map<String, AdminTag> {
        val titles = fetchedPosts.flatMap { it.tags }.map { it.lowercase() }.distinct()

        if (titles.isEmpty()) return emptyMap()

        val existing = tagRepository.findAllByTitleIn(titles)
        val existingTitles = existing.map { it.title.lowercase() }.toHashSet()

        val newTags = titles
            .asSequence()
            .filterNot { it in existingTitles }
            .map { AdminTag(title = it) }
            .toList()
            .toList()

        if (newTags.isEmpty()) {
            return existing.associateBy { it.title }
        }

        val saved = tagRepository.saveAll(newTags).toList()
        return (existing + saved).associateBy { it.title }
    }

    private suspend  fun savePosts(
        techBlog: AdminTechBlog,
        fetchedPosts: List<TechBlogPost>
    ): List<AdminPost> {
        val posts = fetchedPosts.map {
            AdminPost(
                key = it.key,
                title = it.title,
                description = it.description,
                thumbnail = it.thumbnail,
                url = it.url,
                publishedAt = it.publishedAt,
                techBlogId = techBlog.id,
                categoryId = AdminCategory.UNDEFINED.id,
            )
        }
        return postRepository.saveAll(posts).toList()
    }

    private suspend fun savePostTags(
        savedPosts: List<AdminPost>,
        fetchedPosts: List<TechBlogPost>,
        categoriesByTitle: Map<String, AdminTag>
    ) {
        val fetchedByKey = fetchedPosts.associateBy { it.key }

        val postTags = savedPosts.flatMap { savedPost ->
            val fetched = fetchedByKey[savedPost.key]
                ?: throw IllegalStateException("포스트가 존재하지 않습니다.")

            fetched.tags
                .map { it.lowercase() }
                .distinct()
                .map { normalizedTitle ->
                    val tag = categoriesByTitle[normalizedTitle]
                        ?: throw IllegalStateException("카테고리가 존재하지 않습니다. title=$normalizedTitle keys=${categoriesByTitle.keys.take(10)}")
                    AdminPostTag(postId = savedPost.id, tagId = tag.id)
                }
        }

        postTagRepository.saveAll(postTags).toList()
    }
}