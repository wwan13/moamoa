package server.core.feature.subscription.application

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import server.core.feature.member.domain.MemberRepository
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.fixture.createSubscription
import server.core.infra.event.TransactionalEventPublisher
import server.lock.KeyedLock
import test.UnitTest

class SubscriptionServiceTest : UnitTest() {
    @Test
    fun `구독중이 아니면 구독을 생성한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
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

        val result = service.subscribe(command, memberId)

        result.subscribing shouldBe true
        savedSlot.captured.memberId shouldBe memberId
        savedSlot.captured.techBlogId shouldBe command.techBlogId
        savedSlot.captured.notificationEnabled shouldBe true
    }

    @Test
    fun `구독중이 아니면 구독 이벤트를 발행한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

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

        service.subscribe(command, memberId)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<TechBlogSubscribeUpdatedEvent> {
                    it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        it.subscribed
                },
                any()
            )
        }
    }

    @Test
    fun `이미 구독중이면 구독을 추가로 생성하지 않는다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns true
        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing

        val result = service.subscribe(command, memberId)

        result.subscribing shouldBe true
        coVerify(exactly = 0) { subscriptionRepository.save(any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `구독중이면 구독을 해제한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.delete(existing) } returns Unit

        val result = service.unsubscribe(command, memberId)

        result.subscribing shouldBe false
        coVerify(exactly = 1) { subscriptionRepository.delete(existing) }
    }

    @Test
    fun `구독중이면 구독 해제 이벤트를 발행한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing
        coEvery { subscriptionRepository.delete(existing) } returns Unit

        service.unsubscribe(command, memberId)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<TechBlogSubscribeUpdatedEvent> {
                    it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        !it.subscribed
                },
                any()
            )
        }
    }

    @Test
    fun `구독중이 아니면 구독 해제 요청은 false를 반환한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null

        val result = service.unsubscribe(command, memberId)

        result.subscribing shouldBe false
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `존재하지 않는 사용자면 구독 시 예외가 발생한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            service.subscribe(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 사용자 입니다."
        coVerify(exactly = 0) { techBlogRepository.existsById(any()) }
        coVerify(exactly = 0) { subscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `존재하지 않는 기술 블로그면 구독 시 예외가 발생한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

        coEvery { memberRepository.existsById(memberId) } returns true
        coEvery { techBlogRepository.existsById(command.techBlogId) } returns false

        val exception = shouldThrow<IllegalArgumentException> {
            service.subscribe(command, memberId)
        }

        exception.message shouldBe "존재하지 않는 기술 블로그 입니다."
        coVerify(exactly = 0) { subscriptionRepository.findByMemberIdAndTechBlogId(any(), any()) }
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `구독중이 아니면 알림 활성화 시 예외가 발생한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.enableNotification(command, memberId)
        }

        exception.message shouldBe "구독중이지 않은 기술 블로그 입니다."
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `구독중이 아니면 알림 비활성화 시 예외가 발생한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns null

        val exception = shouldThrow<IllegalArgumentException> {
            service.disableNotification(command, memberId)
        }

        exception.message shouldBe "구독중이지 않은 기술 블로그 입니다."
        verify(exactly = 0) { eventPublisher.publish(any(), any()) }
    }

    @Test
    fun `구독 알림을 활성화한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing

        val result = service.enableNotification(command, memberId)

        result.notificationEnabled shouldBe true
        existing.notificationEnabled shouldBe true
    }

    @Test
    fun `구독 알림 활성화 이벤트를 발행한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = false,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing

        service.enableNotification(command, memberId)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<NotificationUpdatedEvent> {
                    it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        it.enabled
                },
                any()
            )
        }
    }

    @Test
    fun `구독 알림을 비활성화한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing

        val result = service.disableNotification(command, memberId)

        result.notificationEnabled shouldBe false
        existing.notificationEnabled shouldBe false
    }

    @Test
    fun `구독 알림 비활성화 이벤트를 발행한다`() = runTest {
        val subscriptionRepository = mockk<SubscriptionRepository>()
        val techBlogRepository = mockk<TechBlogRepository>()
        val memberRepository = mockk<MemberRepository>()
        val eventPublisher = mockk<TransactionalEventPublisher>(relaxed = true)
        val service = createService(
            subscriptionRepository = subscriptionRepository,
            techBlogRepository = techBlogRepository,
            memberRepository = memberRepository,
            eventPublisher = eventPublisher,
        )

        val memberId = 1L
        val command = SubscriptionCommand(techBlogId = 10L)
        val existing = createSubscription(
            id = 22L,
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )

        coEvery { subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) } returns existing

        service.disableNotification(command, memberId)

        verify(exactly = 1) {
            eventPublisher.publish(
                match<NotificationUpdatedEvent> {
                    it.memberId == memberId &&
                        it.techBlogId == command.techBlogId &&
                        !it.enabled
                },
                any()
            )
        }
    }

    private fun createService(
        subscriptionRepository: SubscriptionRepository,
        techBlogRepository: TechBlogRepository,
        memberRepository: MemberRepository,
        keyedLock: KeyedLock = passThroughKeyedLock(),
        eventPublisher: TransactionalEventPublisher = mockk(relaxed = true),
    ): SubscriptionService = SubscriptionService(
        subscriptionRepository = subscriptionRepository,
        techBlogRepository = techBlogRepository,
        memberRepository = memberRepository,
        keyedLock = keyedLock,
        eventPublisher = eventPublisher,
    )

    private fun passThroughKeyedLock(): KeyedLock = object : KeyedLock {
        override fun <T> withLock(key: String, block: () -> T): T = block()
    }
}
