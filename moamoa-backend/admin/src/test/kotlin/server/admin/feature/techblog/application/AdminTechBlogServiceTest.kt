package server.admin.feature.techblog.application

import server.admin.feature.bookmark.domain.AdminBookmarkRepository
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import org.junit.jupiter.api.Test
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
import server.core.feature.category.domain.Category
import server.techblog.TechBlogPost
import server.techblog.TechBlogPostCategory
import server.techblog.TechBlogPostCatetorizer
import test.UnitTest
import java.time.LocalDateTime
import java.util.Optional

class AdminTechBlogServiceTest : UnitTest() {

    @Test
    fun `기술 블로그 전체 목록을 조회한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val subscriptionRepository = mockk<AdminSubscriptionRepository>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = mockk(),
            subscriptionRepository = subscriptionRepository,
            techBlogCollector = mockk(),
            tagRepository = mockk(),
            postTagRepository = mockk(),
            techBlogCategorizer = mockk(),
        )

        every { techBlogRepository.findAllByOrderByTitleAscIdAsc() } returns listOf(
            createAdminTechBlog(id = 1L, title = "가비아", key = "gabia"),
            createAdminTechBlog(id = 2L, title = "카카오", key = "kakao"),
        )
        every { postRepository.countByTechBlogIds(listOf(1L, 2L)) } returns listOf(
            countPost(1L, 11L),
            countPost(2L, 22L),
        )
        every { subscriptionRepository.countByTechBlogIds(listOf(1L, 2L)) } returns listOf(
            countSubscription(1L, 3L),
            countSubscription(2L, 7L),
        )

        val result = service.findAll()

        result.map { it.key } shouldBe listOf("gabia", "kakao")
        result.map { it.postCount } shouldBe listOf(11L, 22L)
        result.map { it.subscriptionCount } shouldBe listOf(3L, 7L)
    }

    @Test
    fun `포스트 수집 결과를 저장하고 개수를 반환한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val tagRepository = mockk<AdminTagRepository>()
        val postTagRepository = mockk<AdminPostTagRepository>()
        val techBlogCategorizer = mockk<TechBlogPostCatetorizer>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = mockk(),
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = tagRepository,
            postTagRepository = postTagRepository,
            techBlogCategorizer = techBlogCategorizer,
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")
        val fetchedPosts = listOf(
            createTechBlogPost(key = "p1", tags = listOf("Kotlin")),
            createTechBlogPost(key = "p2", tags = listOf("Spring")),
        )
        val categorizedPosts = listOf(
            fetchedPosts[0].copy(category = TechBlogPostCategory.ENGINEERING),
            fetchedPosts[1].copy(category = TechBlogPostCategory.PRODUCT),
        )
        val savedPosts = slot<Iterable<AdminPost>>()

        every { techBlogRepository.findById(techBlog.id) } returns Optional.of(techBlog)
        every { techBlogCategorizer.categorize(fetchedPosts[0]) } returns categorizedPosts[0]
        every { techBlogCategorizer.categorize(fetchedPosts[1]) } returns categorizedPosts[1]
        every { tagRepository.findAllByTitleIn(listOf("kotlin", "spring")) } returns emptyList()
        every { tagRepository.saveAll(any<Iterable<AdminTag>>()) } answers {
            firstArg<Iterable<AdminTag>>().toList().mapIndexed { index, tag ->
                AdminTag(id = index + 1L, title = tag.title)
            }
        }
        every { postRepository.findAllByTechBlogIdAndKeyIn(techBlog.id, listOf("p1", "p2")) } returns emptyList()
        every { postRepository.saveAll(capture(savedPosts)) } answers {
            savedPosts.captured.toList().mapIndexed { index, post ->
                createAdminPost(
                    id = 100L + index,
                    key = post.key,
                    title = post.title,
                    description = post.description,
                    thumbnail = post.thumbnail,
                    url = post.url,
                    publishedAt = post.publishedAt,
                    techBlogId = post.techBlogId,
                    categoryId = post.categoryId,
                )
            }
        }
        every { postTagRepository.findAllByPostIdIn(listOf(100L, 101L)) } returns emptyList()
        every { postTagRepository.saveAll(any<Iterable<AdminPostTag>>()) } answers {
            firstArg<Iterable<AdminPostTag>>().toList()
        }

        val result = service.createPosts(techBlog.id, fetchedPosts)

        result.techBlog.id shouldBe techBlog.id
        result.newPostCount shouldBe 2
        result.updatedPostCount shouldBe 0
        savedPosts.captured.toList().map { it.categoryId } shouldBe listOf(
            TechBlogPostCategory.ENGINEERING.categoryId,
            TechBlogPostCategory.PRODUCT.categoryId,
        )
        verify(exactly = 1) { postRepository.saveAll(any<Iterable<AdminPost>>()) }
        verify(exactly = 1) { postTagRepository.saveAll(any<Iterable<AdminPostTag>>()) }
        verify(exactly = 1) { techBlogCategorizer.categorize(fetchedPosts[0]) }
        verify(exactly = 1) { techBlogCategorizer.categorize(fetchedPosts[1]) }
    }

    @Test
    fun `포스트 수집 시 기존 포스트를 업데이트하고 categoryId는 유지한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val tagRepository = mockk<AdminTagRepository>()
        val postTagRepository = mockk<AdminPostTagRepository>()
        val techBlogCategorizer = mockk<TechBlogPostCatetorizer>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = mockk(),
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = tagRepository,
            postTagRepository = postTagRepository,
            techBlogCategorizer = techBlogCategorizer,
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")
        val existingPost = createAdminPost(
            id = 11L,
            key = "p1",
            title = "old title",
            description = "old description",
            thumbnail = "old thumbnail",
            url = "https://old.example.com",
            publishedAt = LocalDateTime.parse("2026-04-01T00:00:00"),
            categoryId = 999L,
        )
        val fetchedPost = createTechBlogPost(
            key = "p1",
            title = "new title",
            description = "new description",
            thumbnail = "new thumbnail",
            url = "https://new.example.com",
            publishedAt = LocalDateTime.parse("2026-04-02T00:00:00"),
            tags = emptyList(),
        )
        val categorizedPost = fetchedPost.copy(category = TechBlogPostCategory.DESIGN)

        every { techBlogRepository.findById(techBlog.id) } returns Optional.of(techBlog)
        every { techBlogCategorizer.categorize(fetchedPost) } returns categorizedPost
        every { tagRepository.findAllByTitleIn(any<List<String>>()) } returns emptyList()
        every { postRepository.findAllByTechBlogIdAndKeyIn(techBlog.id, listOf("p1")) } returns listOf(existingPost)
        every { postTagRepository.findAllByPostIdIn(listOf(existingPost.id)) } returns emptyList()

        val result = service.createPosts(techBlog.id, listOf(fetchedPost))

        result.newPostCount shouldBe 0
        result.updatedPostCount shouldBe 1
        existingPost.title shouldBe fetchedPost.title
        existingPost.description shouldBe fetchedPost.description
        existingPost.thumbnail shouldBe fetchedPost.thumbnail
        existingPost.url shouldBe fetchedPost.url
        existingPost.publishedAt shouldBe fetchedPost.publishedAt
        existingPost.categoryId shouldBe 999L
        verify(exactly = 0) { postRepository.saveAll(any<Iterable<AdminPost>>()) }
        verify(exactly = 1) { techBlogCategorizer.categorize(fetchedPost) }
    }

    @Test
    fun `포스트 수집 시 기존 post tag를 현재 수집 결과 기준으로 동기화한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val tagRepository = mockk<AdminTagRepository>()
        val postTagRepository = mockk<AdminPostTagRepository>()
        val techBlogCategorizer = mockk<TechBlogPostCatetorizer>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = mockk(),
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = tagRepository,
            postTagRepository = postTagRepository,
            techBlogCategorizer = techBlogCategorizer,
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")
        val existingPost = createAdminPost(id = 11L, key = "p1")
        val fetchedPost = createTechBlogPost(key = "p1", tags = listOf("spring", "kotlin"))
        val existingPostTags = listOf(
            AdminPostTag(id = 1L, postId = 11L, tagId = 10L),
            AdminPostTag(id = 2L, postId = 11L, tagId = 30L),
        )
        val deleteSlot = slot<Iterable<AdminPostTag>>()
        val saveSlot = slot<Iterable<AdminPostTag>>()

        every { techBlogRepository.findById(techBlog.id) } returns Optional.of(techBlog)
        every { techBlogCategorizer.categorize(fetchedPost) } returns fetchedPost
        every { tagRepository.findAllByTitleIn(listOf("spring", "kotlin")) } returns listOf(
            AdminTag(id = 10L, title = "spring"),
            AdminTag(id = 20L, title = "kotlin"),
        )
        every { postRepository.findAllByTechBlogIdAndKeyIn(techBlog.id, listOf("p1")) } returns listOf(existingPost)
        every { postTagRepository.findAllByPostIdIn(listOf(11L)) } returns existingPostTags
        every { postTagRepository.deleteAll(capture(deleteSlot)) } just runs
        every { postTagRepository.saveAll(capture(saveSlot)) } answers {
            saveSlot.captured.toList()
        }

        service.createPosts(techBlog.id, listOf(fetchedPost))

        deleteSlot.captured.toList() shouldContainExactlyInAnyOrder listOf(existingPostTags[1])
        saveSlot.captured.toList().map { it.postId to it.tagId } shouldContainExactlyInAnyOrder listOf(11L to 20L)
    }

    @Test
    fun `기술 블로그별 게시글 전체 삭제 시 post tag를 먼저 삭제하고 삭제 건수를 반환한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val bookmarkRepository = mockk<AdminBookmarkRepository>()
        val postTagRepository = mockk<AdminPostTagRepository>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = bookmarkRepository,
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = mockk(),
            postTagRepository = postTagRepository,
            techBlogCategorizer = mockk(),
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")
        val postIds = listOf(11L, 12L)

        every { techBlogRepository.findById(techBlog.id) } returns Optional.of(techBlog)
        every { postRepository.findIdsByTechBlogId(techBlog.id) } returns postIds
        every { postTagRepository.deleteAllByPostIdIn(postIds) } returns 3
        every { bookmarkRepository.deleteAllByPostIdIn(postIds) } returns 4
        every { postRepository.deleteAllByTechBlogId(techBlog.id) } returns 2

        val result = service.deletePosts(techBlog.id)

        result.techBlog.id shouldBe techBlog.id
        result.deletedPostCount shouldBe 2
        verify(exactly = 1) { postTagRepository.deleteAllByPostIdIn(postIds) }
        verify(exactly = 1) { bookmarkRepository.deleteAllByPostIdIn(postIds) }
        verify(exactly = 1) { postRepository.deleteAllByTechBlogId(techBlog.id) }
    }

    @Test
    fun `기술 블로그별 게시글이 없으면 삭제 쿼리를 실행하지 않는다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val postRepository = mockk<AdminPostRepository>()
        val bookmarkRepository = mockk<AdminBookmarkRepository>()
        val postTagRepository = mockk<AdminPostTagRepository>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = postRepository,
            bookmarkRepository = bookmarkRepository,
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = mockk(),
            postTagRepository = postTagRepository,
            techBlogCategorizer = mockk(),
        )
        val techBlog = createAdminTechBlog(id = 1L, key = "gabia")

        every { techBlogRepository.findById(techBlog.id) } returns Optional.of(techBlog)
        every { postRepository.findIdsByTechBlogId(techBlog.id) } returns emptyList()

        val result = service.deletePosts(techBlog.id)

        result.deletedPostCount shouldBe 0
        verify(exactly = 0) { postTagRepository.deleteAllByPostIdIn(any()) }
        verify(exactly = 0) { bookmarkRepository.deleteAllByPostIdIn(any()) }
        verify(exactly = 0) { postRepository.deleteAllByTechBlogId(any()) }
    }

    @Test
    fun `저장 대상 tech blog가 존재하지 않으면 예외가 발생한다`() {
        val techBlogRepository = mockk<AdminTechBlogRepository>()
        val service = AdminTechBlogService(
            techBlogRepository = techBlogRepository,
            postRepository = mockk(),
            bookmarkRepository = mockk(),
            subscriptionRepository = mockk(),
            techBlogCollector = mockk(),
            tagRepository = mockk(),
            postTagRepository = mockk(),
            techBlogCategorizer = mockk(),
        )

        every { techBlogRepository.findById(1L) } returns Optional.empty()

        val exception = shouldThrow<NoSuchElementException> {
            service.createPosts(1L, emptyList())
        }

        exception.message shouldBe "존재하지 않는 tech blog 입니다."
    }

    private fun createAdminTechBlog(
        id: Long = 0L,
        title: String = "가비아",
        key: String = "gabia",
        blogUrl: String = "https://library.gabia.com",
        icon: String = "https://library.gabia.com/icon.png",
    ) = AdminTechBlog(
        id = id,
        title = title,
        key = key,
        blogUrl = blogUrl,
        icon = icon,
    )

    private fun createAdminPost(
        id: Long = 0L,
        key: String = "key",
        title: String = "title",
        description: String = "description",
        thumbnail: String = "thumbnail",
        url: String = "https://example.com",
        publishedAt: LocalDateTime = LocalDateTime.now(),
        techBlogId: Long = 1L,
        categoryId: Long = Category.UNDEFINED.id,
    ) = AdminPost(
        id = id,
        key = key,
        title = title,
        description = description,
        thumbnail = thumbnail,
        url = url,
        publishedAt = publishedAt,
        techBlogId = techBlogId,
        categoryId = categoryId,
    )

    private fun createTechBlogPost(
        key: String = "key",
        title: String = "title",
        description: String = "description",
        thumbnail: String = "thumbnail",
        url: String = "https://example.com",
        publishedAt: java.time.LocalDateTime = java.time.LocalDateTime.parse("2026-04-01T00:00:00"),
        tags: List<String> = emptyList(),
    ) = TechBlogPost(
        key = key,
        title = title,
        description = description,
        tags = tags,
        thumbnail = thumbnail,
        publishedAt = publishedAt,
        url = url,
    )

    private fun countPost(techBlogId: Long, count: Long) = object : AdminPostCountProjection {
        override val techBlogId: Long = techBlogId
        override val count: Long = count
    }

    private fun countSubscription(techBlogId: Long, count: Long) = object : AdminSubscriptionCountProjection {
        override val techBlogId: Long = techBlogId
        override val count: Long = count
    }
}
