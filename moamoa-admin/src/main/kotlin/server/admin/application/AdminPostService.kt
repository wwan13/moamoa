package server.admin.application

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.admin.domain.category.AdminCategory
import server.admin.domain.category.AdminCategoryRepository
import server.admin.domain.post.AdminPost
import server.admin.domain.post.AdminPostRepository
import server.admin.domain.postcategory.AdminPostCategory
import server.admin.domain.postcategory.AdminPostCategoryRepository
import server.admin.domain.techblog.AdminTechBlog
import server.admin.domain.techblog.AdminTechBlogRepository
import server.admin.infra.db.AdminTransactional
import server.techblog.TechBlogPost
import server.techblog.TechBlogSources

@Service
class AdminPostService(
    private val transactional: AdminTransactional,
    private val postRepository: AdminPostRepository,
    private val techBlogRepository: AdminTechBlogRepository,
    private val categoryRepository: AdminCategoryRepository,
    private val postCategoryRepository: AdminPostCategoryRepository,
    private val techBlogSources: TechBlogSources
) {
    suspend fun initPosts(command: AdminInitPostsCommand): AdminInitPostsResult {
        if (postRepository.existsByTechBlogId(command.techBlogId)) {
            throw IllegalArgumentException("이미 초기화된 tech blog 입니다.")
        }

        val techBlog = techBlogRepository.findById(command.techBlogId)
            ?: throw IllegalArgumentException("존재하지 않는 tech blog 입니다.")

        val techBlogClient = techBlogSources.get(techBlog.key)
        val fetchedPosts = techBlogClient.getPosts().toList()

        return transactional {
            val categoriesByTitle = upsertCategories(fetchedPosts)
            val savedPosts = savePosts(techBlog, fetchedPosts)
            savePostCategories(savedPosts, fetchedPosts, categoriesByTitle)

            AdminInitPostsResult(
                techBlog = AdminTechBlogData(techBlog),
                newPostCount = savedPosts.size,
            )
        }
    }

    private suspend  fun upsertCategories(fetchedPosts: List<TechBlogPost>): Map<String, AdminCategory> {
        val titles = fetchedPosts.flatMap { it.categories }.map { it.lowercase() }.distinct()

        if (titles.isEmpty()) return emptyMap()

        val existing = categoryRepository.findAllByTitleIn(titles)
        val existingTitles = existing.map { it.title.lowercase() }.toHashSet()

        val newCategories = titles
            .asSequence()
            .filterNot { it in existingTitles }
            .map { AdminCategory(title = it) }
            .toList()

        if (newCategories.isEmpty()) {
            return existing.associateBy { it.title }
        }

        val saved = categoryRepository.saveAll(newCategories).toList()
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
                thumbnail = it.thumbnail ?: techBlog.icon,
                url = it.url,
                publishedAt = it.publishedAt,
                techBlogId = techBlog.id
            )
        }
        return postRepository.saveAll(posts).toList()
    }

    private suspend fun savePostCategories(
        savedPosts: List<AdminPost>,
        fetchedPosts: List<TechBlogPost>,
        categoriesByTitle: Map<String, AdminCategory>
    ) {
        val fetchedByKey = fetchedPosts.associateBy { it.key }

        val postCategories = savedPosts.flatMap { savedPost ->
            val fetched = fetchedByKey[savedPost.key]
                ?: throw IllegalStateException("포스트가 존재하지 않습니다.")

            fetched.categories
                .map { it.lowercase() }
                .distinct()
                .map { normalizedTitle ->
                    val category = categoriesByTitle[normalizedTitle]
                        ?: throw IllegalStateException("카테고리가 존재하지 않습니다. title=$normalizedTitle keys=${categoriesByTitle.keys.take(10)}")
                    AdminPostCategory(postId = savedPost.id, categoryId = category.id)
                }
        }

        postCategoryRepository.saveAll(postCategories).toList()
    }
}