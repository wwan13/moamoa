package server.core.feature.subscription.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.core.feature.member.domain.MemberRepository
import server.core.feature.techblog.application.TechBlogData
import server.core.feature.techblog.domain.TechBlogRepository
import server.core.feature.subscription.domain.Subscription
import server.core.feature.subscription.domain.SubscriptionRepository
import server.core.infra.db.transaction.Transactional
import server.global.logging.event

@Service
class SubscriptionService(
    private val transactional: Transactional,
    private val subscriptionRepository: SubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: server.lock.KeyedLock
) {
    private val logger = KotlinLogging.logger {}

    fun toggle(
        command: SubscriptionToggleCommand,
        memberId: Long
    ): SubscriptionToggleResult {
        val mutexKey = "subscriptionToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            transactional {
                if (!memberRepository.existsById(memberId)) {
                    throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
                }
                if (!techBlogRepository.existsById(command.techBlogId)) {
                    throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
                }

                subscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                    ?.let { subscription ->
                        subscriptionRepository.deleteById(subscription.id)

                        val event = subscription.unsubscribe()
                        registerEvent(event)
                        logger.event.info(event) {
                            "기술 블로그 구독 해제 이벤트를 발행했습니다"
                        }

                        SubscriptionToggleResult(false)
                    }
                    ?: let {
                        val subscription = Subscription(
                            notificationEnabled = true,
                            memberId = memberId,
                            techBlogId = command.techBlogId
                        )
                        val saved = subscriptionRepository.save(subscription)

                        val event = saved.subscribe()
                        registerEvent(event)
                        logger.event.info(event) {
                            "기술 블로그 구독 등록 이벤트를 발행했습니다"
                        }

                        SubscriptionToggleResult(true)
                    }

            }
        }
    }

    fun notificationEnabledToggle(
        command: NotificationEnabledToggleCommand,
        memberId: Long
    ): NotificationEnabledToggleResult {
        val mutexKey = "notificationEnabledToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            transactional {
                val subscription = subscriptionRepository
                    .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                    ?: throw IllegalArgumentException("구독중이지 않은 기술 블로그 입니다.")

                val event = subscription.toggleNotification()
                registerEvent(event)
                logger.event.info(event) {
                    "기술 블로그 알림 설정 변경 이벤트를 발행했습니다"
                }

                NotificationEnabledToggleResult(subscription.notificationEnabled)
            }
        }
    }
}
