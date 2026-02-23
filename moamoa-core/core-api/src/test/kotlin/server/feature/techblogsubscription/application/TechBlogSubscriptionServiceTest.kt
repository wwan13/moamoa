package server.feature.techblogsubscription.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.feature.member.command.domain.MemberRepository
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.NotificationUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscribeUpdatedEvent
import server.feature.techblogsubscription.domain.TechBlogSubscription
import server.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.shared.lock.KeyedLock
import server.infra.db.transaction.TransactionScope
import server.infra.db.transaction.Transactional
import server.fixture.createTechBlog
import server.fixture.createTechBlogSubscription
import test.UnitTest

class TechBlogSubscriptionServiceTest : UnitTest() {
    @Test
    fun `구독중이 아니면 구독을 생성한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)
        val savedSlot = slot<TechBlogSubscription>()

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { techBlogSubscriptionRepository.save(capture(savedSlot)) } coAnswers {
            savedSlot.captured.copy(id = 101L)
        }
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.subscribing shouldBe true
        savedSlot.captured.memberId shouldBe memberId
        savedSlot.captured.techBlogId shouldBe command.techBlogId
        savedSlot.captured.notificationEnabled shouldBe true
    }

    @Test
    fun `구독중이 아니면 구독 이벤트를 발행하고 subscribing은 true이다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { techBlogSubscriptionRepository.save(any()) } coAnswers {
            firstArg<TechBlogSubscription>().copy(id = 101L)
        }
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is TechBlogSubscribeUpdatedEvent &&
                        it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        it.subscribed
                }
            )
        }
    }

    @Test
    fun `구독중이면 구독을 해제한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.subscribing shouldBe false
        coVerify(exactly = 1) { techBlogSubscriptionRepository.deleteById(existing.id) }
    }

    @Test
    fun `구독중이면 구독 해제 이벤트를 발행하고 subscribing은 false이다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        service.toggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is TechBlogSubscribeUpdatedEvent &&
                        it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        !it.subscribed
                }
            )
        }
    }

    @Test
    fun `존재하지 않는 사용자면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns false
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        coVerify(exactly = 0) { techBlogRepository.existsById(any()) }
        coVerify(exactly = 0) { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
    }

    @Test
    fun `존재하지 않는 기술 블로그면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = TechBlogSubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns false
        coEvery { transactional.invoke<TechBlogSubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> TechBlogSubscriptionToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 기술 블로그 입니다."
        coVerify(exactly = 0) { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
    }

    @Test
    fun `구독중이 아니면 구독 알림 토글 시 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)

        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.notificationEnabledToggle(command, memberId)
        }

        exception.message shouldBe "구독중이지 않은 기술 블로그 입니다."
    }

    @Test
    fun `구독 알림이 켜져 있으면 끄도록 토글한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )
        val savedSlot = slot<TechBlogSubscription>()

        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.save(capture(savedSlot)) } coAnswers {
            savedSlot.captured
        }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        val result = service.notificationEnabledToggle(command, memberId)

        result.notificationEnabled shouldBe false
        savedSlot.captured.notificationEnabled shouldBe false
    }

    @Test
    fun `구독 알림이 켜져 있으면 끄는 이벤트를 발행한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.save(any()) } coAnswers { firstArg() }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        service.notificationEnabledToggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is NotificationUpdatedEvent &&
                        it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        !it.enabled
                }
            )
        }
    }

    @Test
    fun `구독 알림이 꺼져 있으면 켜도록 토글한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )
        val savedSlot = slot<TechBlogSubscription>()

        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.save(capture(savedSlot)) } coAnswers {
            savedSlot.captured
        }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        val result = service.notificationEnabledToggle(command, memberId)

        result.notificationEnabled shouldBe true
        savedSlot.captured.notificationEnabled shouldBe true
    }

    @Test
    fun `구독 알림이 꺼져 있으면 켜는 이벤트를 발행한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createTechBlogSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { techBlogSubscriptionRepository.save(any()) } coAnswers { firstArg() }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<suspend TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        service.notificationEnabledToggle(command, memberId)

        coVerify(exactly = 1) {
            transactionScope.registerEvent(
                match {
                    it is NotificationUpdatedEvent &&
                        it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        it.enabled
                }
            )
        }
    }

    @Test
    fun `구독중인 기술 블로그가 없으면 빈 결과를 반환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L

        every { techBlogSubscriptionRepository.findAllByMemberId(memberId) } returns emptyFlow()

        val result = service.subscribingTechBlogs(memberId).toList()

        result shouldBe emptyList()
        verify(exactly = 0) { techBlogRepository.findAllById(any<Iterable<Long>>()) }
    }

    @Test
    fun `구독중인 기술 블로그가 있으면 TechBlogData로 변환한다`() = runTest {
        val transactional = mockk<Transactional>()
        val techBlogSubscriptionRepository = mockk<TechBlogSubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val keyedLock = passThroughKeyedLock()
        val service = TechBlogSubscriptionService(
            transactional,
            techBlogSubscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val subscriptions = listOf(
            createTechBlogSubscription(
                id = 1L,
                notificationEnabled = true,
                memberId = memberId,
                techBlogId = 10L
            ),
            createTechBlogSubscription(
                id = 2L,
                notificationEnabled = false,
                memberId = memberId,
                techBlogId = 20L
            )
        )
        val techBlogs = listOf(
            createTechBlog(
                id = 10L,
                title = "Blog-10",
                key = "key-10",
                icon = "icon-10",
                blogUrl = "https://blog-10.example.com",
                subscriptionCount = 3L
            ),
            createTechBlog(
                id = 20L,
                title = "Blog-20",
                key = "key-20",
                icon = "icon-20",
                blogUrl = "https://blog-20.example.com",
                subscriptionCount = 5L
            )
        )

        every { techBlogSubscriptionRepository.findAllByMemberId(memberId) } returns flowOf(*subscriptions.toTypedArray())
        every { techBlogRepository.findAllById(listOf(10L, 20L)) } returns flowOf(*techBlogs.toTypedArray())

        val result = service.subscribingTechBlogs(memberId).toList()

        result.map { it.id } shouldBe listOf(10L, 20L)
        result.map { it.key } shouldBe listOf("key-10", "key-20")
    }

    private fun passThroughKeyedLock(): KeyedLock = object : KeyedLock {
        override suspend fun <T> withLock(key: String, block: suspend () -> T): T = block()
    }
}
