package server.core.feature.subscription.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRepository
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.infra.db.transaction.TransactionScope
import server.core.infra.db.transaction.Transactional
import server.core.fixture.createTechBlog
import server.core.fixture.createSubscription
import test.UnitTest

class SubscriptionServiceTest : UnitTest() {
    @Test
    fun `구독중이 아니면 구독을 생성한다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)
        val savedSlot = slot<Subscription>()

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { subscriptionRepository.save(capture(savedSlot)) } coAnswers {
            Subscription(
                id = 101L,
                notificationEnabled = savedSlot.captured.notificationEnabled,
                memberId = savedSlot.captured.memberId,
                techBlogId = savedSlot.captured.techBlogId,
            )
        }
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
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
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { subscriptionRepository.save(any()) } coAnswers {
            firstArg<Subscription>().let {
                Subscription(
                    id = 101L,
                    notificationEnabled = it.notificationEnabled,
                    memberId = it.memberId,
                    techBlogId = it.techBlogId,
                )
            }
        }
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
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
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
            block(transactionScope)
        }

        val result = service.toggle(command, memberId)

        result.subscribing shouldBe false
        coVerify(exactly = 1) { subscriptionRepository.deleteById(existing.id) }
    }

    @Test
    fun `구독중이면 구독 해제 이벤트를 발행하고 subscribing은 false이다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.deleteById(existing.id) } returns Unit
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
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
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns false
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        coVerify(exactly = 0) { techBlogRepository.existsById(any()) }
        coVerify(exactly = 0) { subscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
    }

    @Test
    fun `존재하지 않는 기술 블로그면 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = SubscriptionToggleCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns false
        coEvery { transactional.invoke<SubscriptionToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> SubscriptionToggleResult>()
            block(transactionScope)
        }

        val exception = shouldThrow<IllegalArgumentException> {
            service.toggle(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 기술 블로그 입니다."
        coVerify(exactly = 0) { subscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
    }

    @Test
    fun `구독중이 아니면 구독 알림 토글 시 예외가 발생한다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> NotificationEnabledToggleResult>()
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
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        val result = service.notificationEnabledToggle(command, memberId)

        result.notificationEnabled shouldBe false
        existing.notificationEnabled shouldBe false
    }

    @Test
    fun `구독 알림이 켜져 있으면 끄는 이벤트를 발행한다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.save(any()) } coAnswers { firstArg() }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> NotificationEnabledToggleResult>()
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
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> NotificationEnabledToggleResult>()
            block(transactionScope)
        }

        val result = service.notificationEnabledToggle(command, memberId)

        result.notificationEnabled shouldBe true
        existing.notificationEnabled shouldBe true
    }

    @Test
    fun `구독 알림이 꺼져 있으면 켜는 이벤트를 발행한다`() = runTest {
        val transactional = mockk<Transactional>()
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val transactionScope = mockk<TransactionScope>(relaxed = true)
        val keyedLock = passThroughKeyedLock()
        val service = SubscriptionService(
            transactional,
            subscriptionRepository,
            techBlogRepository,
            memberRepository,
            keyedLock
        )

        val memberId = 1L
        val command = NotificationEnabledToggleCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.save(any()) } coAnswers { firstArg() }
        coEvery { transactional.invoke<NotificationEnabledToggleResult>(any(), any()) } coAnswers {
            val block = secondArg<TransactionScope.() -> NotificationEnabledToggleResult>()
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

    private fun passThroughKeyedLock(): server.lock.KeyedLock = object : server.lock.KeyedLock {
        override fun <T> withLock(key: String, block: () -> T): T = block()
    }
}
