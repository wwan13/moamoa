package server.core.feature.subscription.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.member.domain.MemberRepository
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.feature.subscription.infra.SubscriptionEventPublisher
import server.core.feature.subscription.infra.SubscriptionLock
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.global.security.UnauthorizedException
import server.global.logging.biz

@Service
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val subscriptionLock: SubscriptionLock,
    private val subscriptionEventPublisher: SubscriptionEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    @Transactional
    fun subscribe(
        command: SubscriptionCommand,
        memberId: Long
    ) = subscriptionLock.withSubscriptionLock(memberId, command.techBlogId) {
        if (!memberRepository.existsById(memberId)) {
            throw UnauthorizedException()
        }
        if (!techBlogRepository.existsById(command.techBlogId)) {
            throw NoSuchElementException("존재하지 않는 기술 블로그 입니다.")
        }

        if (subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) != null) {
            return@withSubscriptionLock
        }

        val subscription = Subscription(
            notificationEnabled = true,
            memberId = memberId,
            techBlogId = command.techBlogId
        )
        val saved = subscriptionRepository.save(subscription)
        subscriptionEventPublisher.publishSubscribed(saved)
        logger.biz.info { "기술 블로그를 구독합니다" }
    }

    @Transactional
    fun unsubscribe(
        command: SubscriptionCommand,
        memberId: Long
    ) = subscriptionLock.withSubscriptionLock(memberId, command.techBlogId) {
        val subscription = subscriptionRepository
            .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?: return@withSubscriptionLock

        subscriptionRepository.delete(subscription)
        subscriptionEventPublisher.publishUnsubscribed(subscription)
        logger.biz.info { "기술 블로그 구독을 해제합니다" }
    }

    @Transactional
    fun enableNotification(
        command: SubscriptionCommand,
        memberId: Long
    ) = subscriptionLock.withNotificationLock(memberId, command.techBlogId) {
        val subscription = subscriptionRepository
            .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?: throw NoSuchElementException("구독중이지 않은 기술 블로그 입니다.")

        subscription.enableNotification()
        subscriptionEventPublisher.publishNotificationEnabled(subscription)
        logger.biz.info { "기술 블로그 알림을 활성화합니다" }
    }

    @Transactional
    fun disableNotification(
        command: SubscriptionCommand,
        memberId: Long
    ) = subscriptionLock.withNotificationLock(memberId, command.techBlogId) {
        val subscription = subscriptionRepository
            .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?: throw NoSuchElementException("구독중이지 않은 기술 블로그 입니다.")

        subscription.disableNotification()
        subscriptionEventPublisher.publishNotificationDisabled(subscription)
        logger.biz.info { "기술 블로그 알림을 비활성화합니다" }
    }
}
