package server.feature.post.query

import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.r2dbc.spi.Row
import io.r2dbc.spi.RowMetadata
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import org.springframework.r2dbc.core.DatabaseClient
import org.springframework.r2dbc.core.RowsFetchSpec
import reactor.core.publisher.Flux
import reactor.core.publisher.Mono
import server.feature.member.command.domain.MemberRole
import server.feature.techblog.command.application.TechBlogData
import server.infra.cache.PostListCache
import server.infra.cache.WarmupCoordinator
import server.security.Passport
import test.UnitTest
import java.time.LocalDateTime
import java.util.function.BiFunction

class PostQueryServiceTest : UnitTest() {
    private val warmupCoordinator = mockk<WarmupCoordinator>(relaxed = true)

    init {
        every { warmupCoordinator.launchIfAbsent(any(), any()) } answers {
            runBlocking { secondArg<suspend () -> Unit>().invoke() }
        }
    }
    @Test
    fun `캐시된 게시글이 있으면 캐시 메모리에서 결과를 가져와 결과를 병합한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val memberId = 10L
        val passport = Passport(memberId = memberId, role = MemberRole.USER)
        val conditions = PostQueryConditions(page = 1, size = 20, query = null)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )
        val statsMap = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        val bookmarkedIdSet = setOf(2L)

        mockCount(databaseClient = databaseClient, totalCount = 2L)
        coEvery { postListCache.get(1L, 20L) } returns basePosts
        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns statsMap
        coEvery {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        } returns bookmarkedIdSet

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findByConditions(conditions, passport)

        result.meta shouldBe PostListMeta(
            page = 1,
            size = 20,
            totalCount = 2,
            totalPages = 1
        )
        result.posts.map { it.id } shouldBe listOf(1L, 2L)
        result.posts[0].viewCount shouldBe 10L
        result.posts[0].bookmarkCount shouldBe 11L
        result.posts[0].isBookmarked shouldBe false
        result.posts[1].viewCount shouldBe 3L
        result.posts[1].bookmarkCount shouldBe 4L
        result.posts[1].isBookmarked shouldBe true
    }

    @Test
    fun `캐시된 게시글이 없으면 데이터베이스에서 결과를 가져와 결과를 병합한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val memberId = 10L
        val passport = Passport(memberId = memberId, role = MemberRole.USER)
        val conditions = PostQueryConditions(page = 1, size = 20, query = null)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )
        val statsMap = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )
        val bookmarkedIdSet = setOf(2L)

        mockCountAndList(
            databaseClient = databaseClient,
            totalCount = 2L,
            basePosts = basePosts
        )
        coEvery { postListCache.get(1L, 20L) } returns null
        every { postListCache.key(1L, 20L) } returns "POST:LIST:PAGE:1:SIZE:20"
        coEvery { postListCache.set(1L, 20L, basePosts) } returns Unit
        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns statsMap
        coEvery {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        } returns bookmarkedIdSet

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findByConditions(conditions, passport)
        advanceUntilIdle()

        result.meta shouldBe PostListMeta(
            page = 1,
            size = 20,
            totalCount = 2,
            totalPages = 1
        )
        result.posts.map { it.id } shouldBe listOf(1L, 2L)
        result.posts[0].viewCount shouldBe 10L
        result.posts[0].bookmarkCount shouldBe 11L
        result.posts[0].isBookmarked shouldBe false
        result.posts[1].viewCount shouldBe 3L
        result.posts[1].bookmarkCount shouldBe 4L
        result.posts[1].isBookmarked shouldBe true
        coVerify(exactly = 1) { postListCache.set(1L, 20L, basePosts) }
    }

    @Test
    fun `passport가 null이면 모든 게시글이 북마크되지 않은 상태로 조회된다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val conditions = PostQueryConditions(page = 1, size = 20, query = null)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )
        val statsMap = mapOf(
            1L to PostStats(postId = 1L, viewCount = 10L, bookmarkCount = 11L)
        )

        mockCount(databaseClient = databaseClient, totalCount = 2L)
        coEvery { postListCache.get(1L, 20L) } returns basePosts
        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns statsMap

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        val result = service.findByConditions(conditions, passport = null)

        result.posts.all { !it.isBookmarked } shouldBe true
        coVerify(exactly = 0) { bookmarkedPostReader.findBookmarkedPostIdSet(any(), any()) }
    }

    @Test
    fun `passport가 null이 아니면 북마크 여부를 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val memberId = 10L
        val passport = Passport(memberId = memberId, role = MemberRole.USER)
        val conditions = PostQueryConditions(page = 1, size = 20, query = null)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )

        mockCount(databaseClient = databaseClient, totalCount = 2L)
        coEvery { postListCache.get(1L, 20L) } returns basePosts
        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns emptyMap()
        coEvery {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        } returns emptySet()

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        service.findByConditions(conditions, passport)

        coVerify(exactly = 1) {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        }
    }

    @Test
    fun `검색 키워드가 있으면 캐시를 사용하지 않고 키워드 조건으로 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val memberId = 10L
        val passport = Passport(memberId = memberId, role = MemberRole.USER)
        val conditions = PostQueryConditions(page = 1, size = 20, query = "kotlin")

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )

        val countSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val countRowsFetchSpec = mockk<RowsFetchSpec<Long>>()
        val listSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val listRowsFetchSpec = mockk<RowsFetchSpec<PostSummary>>()

        every { databaseClient.sql(any<String>()) } answers {
            val sql = firstArg<String>()
            if (sql.contains("COUNT")) countSpec else listSpec
        }
        every { countSpec.bind("keyword", "%kotlin%") } returns countSpec
        every { countSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns countRowsFetchSpec
        every { countRowsFetchSpec.one() } returns Mono.just(2L)
        every { listSpec.bind("keyword", "%kotlin%") } returns listSpec
        every { listSpec.bind("limit", any<Long>()) } returns listSpec
        every { listSpec.bind("offset", any<Long>()) } returns listSpec
        every { listSpec.map(any<BiFunction<Row, RowMetadata, PostSummary>>()) } returns listRowsFetchSpec
        every { listRowsFetchSpec.all() } returns Flux.fromIterable(basePosts)

        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns emptyMap()
        coEvery {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        } returns emptySet()

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        service.findByConditions(conditions, passport)

        coVerify(exactly = 0) { postListCache.get(any(), any()) }
        coVerify(exactly = 0) { postListCache.set(any(), any(), any()) }
        verify(exactly = 1) { countSpec.bind("keyword", "%kotlin%") }
        verify(exactly = 1) { listSpec.bind("keyword", "%kotlin%") }
    }

    @Test
    fun `검색 키워드가 없으면 캐시를 조회한다`() = runTest {
        val databaseClient = mockk<DatabaseClient>()
        val postListCache = mockk<PostListCache>()
        val bookmarkedPostReader = mockk<BookmarkedPostReader>()
        val postStatsReader = mockk<PostStatsReader>()

        val memberId = 10L
        val passport = Passport(memberId = memberId, role = MemberRole.USER)
        val conditions = PostQueryConditions(page = 1, size = 20, query = null)

        val basePosts = listOf(
            postSummary(id = 1L, viewCount = 1L, bookmarkCount = 1L, isBookmarked = false),
            postSummary(id = 2L, viewCount = 3L, bookmarkCount = 4L, isBookmarked = false)
        )

        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>(relaxed = true)
        val rowsFetchSpec = mockk<RowsFetchSpec<Long>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.one() } returns Mono.just(2L)

        coEvery { postListCache.get(1L, 20L) } returns basePosts
        coEvery { postStatsReader.findPostStatsMap(listOf(1L, 2L)) } returns emptyMap()
        coEvery {
            bookmarkedPostReader.findBookmarkedPostIdSet(
                memberId = memberId,
                postIds = listOf(1L, 2L)
            )
        } returns emptySet()

        val service = PostQueryService(
            databaseClient = databaseClient,
            postListCache = postListCache,
            bookmarkedPostReader = bookmarkedPostReader,
            postStatsReader = postStatsReader,
            warmupCoordinator = warmupCoordinator
        )

        service.findByConditions(conditions, passport)

        coVerify(exactly = 1) { postListCache.get(1L, 20L) }
        verify(exactly = 0) { executeSpec.bind("limit", any<Long>()) }
        verify(exactly = 0) { executeSpec.bind("offset", any<Long>()) }
    }

    private fun postSummary(
        id: Long,
        viewCount: Long,
        bookmarkCount: Long,
        isBookmarked: Boolean,
    ) = PostSummary(
        id = id,
        key = "post-$id",
        title = "title-$id",
        description = "desc-$id",
        thumbnail = "thumb-$id",
        url = "https://example.com/$id",
        publishedAt = LocalDateTime.of(2024, 1, 1, 0, 0),
        isBookmarked = isBookmarked,
        viewCount = viewCount,
        bookmarkCount = bookmarkCount,
        techBlog = TechBlogData(
            id = 1L,
            title = "blog",
            icon = "icon",
            blogUrl = "https://blog.example.com",
            key = "blog-key",
            subscriptionCount = 0L
        )
    )

    private fun mockCount(
        databaseClient: DatabaseClient,
        totalCount: Long,
    ) {
        val executeSpec = mockk<DatabaseClient.GenericExecuteSpec>()
        val rowsFetchSpec = mockk<RowsFetchSpec<Long>>()

        every { databaseClient.sql(any<String>()) } returns executeSpec
        every { executeSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns rowsFetchSpec
        every { rowsFetchSpec.one() } returns Mono.just(totalCount)
    }

    private fun mockCountAndList(
        databaseClient: DatabaseClient,
        totalCount: Long,
        basePosts: List<PostSummary>,
        query: String? = null,
    ) {
        val countSpec = mockk<DatabaseClient.GenericExecuteSpec>()
        val countRowsFetchSpec = mockk<RowsFetchSpec<Long>>()
        val listSpec = mockk<DatabaseClient.GenericExecuteSpec>()
        val listRowsFetchSpec = mockk<RowsFetchSpec<PostSummary>>()

        every { databaseClient.sql(any<String>()) } answers {
            val sql = firstArg<String>()
            if (sql.contains("COUNT")) countSpec else listSpec
        }

        if (query != null) {
            every { countSpec.bind("keyword", "%$query%") } returns countSpec
        }
        every { countSpec.map(any<BiFunction<Row, RowMetadata, Long>>()) } returns countRowsFetchSpec
        every { countRowsFetchSpec.one() } returns Mono.just(totalCount)

        if (query != null) {
            every { listSpec.bind("keyword", "%$query%") } returns listSpec
        }
        every { listSpec.bind("limit", any<Long>()) } returns listSpec
        every { listSpec.bind("offset", any<Long>()) } returns listSpec
        every { listSpec.map(any<BiFunction<Row, RowMetadata, PostSummary>>()) } returns listRowsFetchSpec
        every { listRowsFetchSpec.all() } returns Flux.fromIterable(basePosts)
    }
}
