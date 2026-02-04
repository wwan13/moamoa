package server.admin.feature.post.application

import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.admin.feature.category.domain.AdminCategory
import server.admin.infra.transaction.AdminTransactional
import server.techblog.TechBlogPost
import server.techblog.TechBlogSources

@Service
internal class AdminPostService(
    private val transactional: AdminTransactional,
    private val postRepository: server.admin.feature.post.domain.AdminPostRepository,
    private val techBlogRepository: server.admin.feature.techblog.domain.AdminTechBlogRepository,
    private val tagRepository: server.admin.feature.tag.domain.AdminTagRepository,
    private val postTagRepository: server.admin.feature.posttag.domain.AdminPostTagRepository,
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
                techBlog = _root_ide_package_.server.admin.feature.techblog.application.AdminTechBlogData(techBlog),
                newPostCount = savedPosts.size,
            )
        }
    }

    private suspend  fun upsertTags(fetchedPosts: List<TechBlogPost>): Map<String, server.admin.feature.tag.domain.AdminTag> {
        val titles = fetchedPosts.flatMap { it.tags }.map { it.lowercase() }.distinct()

        if (titles.isEmpty()) return emptyMap()

        val existing = tagRepository.findAllByTitleIn(titles)
        val existingTitles = existing.map { it.title.lowercase() }.toHashSet()

        val newTags = titles
            .asSequence()
            .filterNot { it in existingTitles }
            .map { _root_ide_package_.server.admin.feature.tag.domain.AdminTag(title = it) }
            .toList()
            .toList()

        if (newTags.isEmpty()) {
            return existing.associateBy { it.title }
        }

        val saved = tagRepository.saveAll(newTags).toList()
        return (existing + saved).associateBy { it.title }
    }

    private suspend  fun savePosts(
        techBlog: server.admin.feature.techblog.domain.AdminTechBlog,
        fetchedPosts: List<TechBlogPost>
    ): List<server.admin.feature.post.domain.AdminPost> {
        val posts = fetchedPosts.map {
            _root_ide_package_.server.admin.feature.post.domain.AdminPost(
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
        savedPosts: List<server.admin.feature.post.domain.AdminPost>,
        fetchedPosts: List<TechBlogPost>,
        categoriesByTitle: Map<String, server.admin.feature.tag.domain.AdminTag>
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
                    _root_ide_package_.server.admin.feature.posttag.domain.AdminPostTag(
                        postId = savedPost.id,
                        tagId = tag.id
                    )
                }
        }

        postTagRepository.saveAll(postTags).toList()
    }
}
