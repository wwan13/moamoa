package server.core.feature.subscription.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import server.core.feature.member.domain.MemberRepository
import server.core.feature.subscription.domain.NotificationUpdatedEvent
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.feature.subscription.domain.TechBlogSubscribeUpdatedEvent
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.global.security.UnauthorizedException
import server.core.infra.event.TransactionalEventPublisher
import server.global.logging.biz
import server.lock.KeyedLock

@Service
@Transactional
class SubscriptionService(
    private val subscriptionRepository: SubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: KeyedLock,
    private val eventPublisher: TransactionalEventPublisher,
) {
    private val logger = KotlinLogging.logger {}

    fun subscribe(
        command: SubscriptionCommand,
        memberId: Long
    ) {
        val mutexKey = "subscription:$memberId:${command.techBlogId}"
        keyedLock.withLock(mutexKey) {
            if (!memberRepository.existsById(memberId)) {
                throw UnauthorizedException()
            }
            if (!techBlogRepository.existsById(command.techBlogId)) {
                throw NoSuchElementException("존재하지 않는 기술 블로그 입니다.")
            }

            if (subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId) != null) {
                return@withLock
            }

            val subscription = Subscription(
                notificationEnabled = true,
                memberId = memberId,
                techBlogId = command.techBlogId
            )
            val saved = subscriptionRepository.save(subscription)
            eventPublisher.publish(
                TechBlogSubscribeUpdatedEvent(
                    memberId = saved.memberId,
                    techBlogId = saved.techBlogId,
                    subscribed = true,
                )
            )
            logger.biz.info { "기술 블로그를 구독합니다" }
        }
    }

    fun unsubscribe(
        command: SubscriptionCommand,
        memberId: Long
    ) {
        val mutexKey = "subscription:$memberId:${command.techBlogId}"
        keyedLock.withLock(mutexKey) {
            val subscription = subscriptionRepository
                .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                ?: return@withLock

            subscriptionRepository.delete(subscription)
            eventPublisher.publish(
                TechBlogSubscribeUpdatedEvent(
                    memberId = subscription.memberId,
                    techBlogId = subscription.techBlogId,
                    subscribed = false,
                )
            )
            logger.biz.info { "기술 블로그 구독을 해제합니다" }
        }
    }

    fun enableNotification(
        command: SubscriptionCommand,
        memberId: Long
    ) {
        val mutexKey = "notificationEnabled:$memberId:${command.techBlogId}"
        keyedLock.withLock(mutexKey) {
            val subscription = subscriptionRepository
                .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                ?: throw NoSuchElementException("구독중이지 않은 기술 블로그 입니다.")

            subscription.enableNotification()
            eventPublisher.publish(
                NotificationUpdatedEvent(
                    memberId = subscription.memberId,
                    techBlogId = subscription.techBlogId,
                    enabled = true,
                )
            )
            logger.biz.info { "기술 블로그 알림을 활성화합니다" }
        }
    }

    fun disableNotification(
        command: SubscriptionCommand,
        memberId: Long
    ) {
        val mutexKey = "notificationEnabled:$memberId:${command.techBlogId}"
        keyedLock.withLock(mutexKey) {
            val subscription = subscriptionRepository
                .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                ?: throw NoSuchElementException("구독중이지 않은 기술 블로그 입니다.")

            subscription.disableNotification()
            eventPublisher.publish(
                NotificationUpdatedEvent(
                    memberId = subscription.memberId,
                    techBlogId = subscription.techBlogId,
                    enabled = false,
                )
            )
            logger.biz.info { "기술 블로그 알림을 비활성화합니다" }
        }
    }
}
