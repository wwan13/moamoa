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

    fun toggle(
        command: SubscriptionToggleCommand,
        memberId: Long
    ): SubscriptionToggleResult {
        val mutexKey = "subscriptionToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            if (!memberRepository.existsById(memberId)) {
                throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
            }
            if (!techBlogRepository.existsById(command.techBlogId)) {
                throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
            }

            subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                ?.let { subscription ->
                    subscriptionRepository.delete(subscription)
                    eventPublisher.publish(
                        TechBlogSubscribeUpdatedEvent(
                            memberId = subscription.memberId,
                            techBlogId = subscription.techBlogId,
                            subscribed = false,
                        )
                    )
                    logger.biz.info { "기술 블로그 구독을 해제합니다" }

                    SubscriptionToggleResult(false)
                }
                ?: let {
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

                    SubscriptionToggleResult(true)
                }
        }
    }

    fun notificationEnabledToggle(
        command: NotificationEnabledToggleCommand,
        memberId: Long
    ): NotificationEnabledToggleResult {
        val mutexKey = "notificationEnabledToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            val subscription = subscriptionRepository
                .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                ?: throw IllegalArgumentException("구독중이지 않은 기술 블로그 입니다.")

            subscription.toggleNotification()
            eventPublisher.publish(
                NotificationUpdatedEvent(
                    memberId = subscription.memberId,
                    techBlogId = subscription.techBlogId,
                    enabled = subscription.notificationEnabled,
                )
            )
            logger.biz.info { "기술 블로그 알림 설정을 변경합니다" }

            NotificationEnabledToggleResult(subscription.notificationEnabled)
        }
    }
}
