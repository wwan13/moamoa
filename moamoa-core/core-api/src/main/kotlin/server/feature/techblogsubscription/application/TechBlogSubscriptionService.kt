package server.feature.techblogsubscription.application

import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.stereotype.Service
import server.feature.member.command.domain.MemberRepository
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.TechBlogSubscription
import server.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.global.logging.event
import server.infra.db.transaction.Transactional
import server.shared.lock.KeyedLock

@Service
class TechBlogSubscriptionService(
    private val transactional: Transactional,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val keyedLock: KeyedLock
) {
    private val logger = KotlinLogging.logger {}

    fun toggle(
        command: TechBlogSubscriptionToggleCommand,
        memberId: Long
    ): TechBlogSubscriptionToggleResult {
        val mutexKey = "techBlogSubscriptionToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            transactional {
                if (!memberRepository.existsById(memberId)) {
                    throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
                }
                if (!techBlogRepository.existsById(command.techBlogId)) {
                    throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
                }

                techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                    ?.let { subscription ->
                        techBlogSubscriptionRepository.deleteById(subscription.id)

                        val event = subscription.unsubscribe()
                        registerEvent(event)
                        logger.event.info(event) {
                            "기술 블로그 구독 해제 이벤트를 발행했습니다"
                        }

                        TechBlogSubscriptionToggleResult(false)
                    }
                    ?: let {
                        val subscription = TechBlogSubscription(
                            notificationEnabled = true,
                            memberId = memberId,
                            techBlogId = command.techBlogId
                        )
                        val saved = techBlogSubscriptionRepository.save(subscription)

                        val event = saved.subscribe()
                        registerEvent(event)
                        logger.event.info(event) {
                            "기술 블로그 구독 등록 이벤트를 발행했습니다"
                        }

                        TechBlogSubscriptionToggleResult(true)
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
                val subscription = techBlogSubscriptionRepository
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

    fun subscribingTechBlogs(memberId: Long): List<TechBlogData> {
        val subscriptions = techBlogSubscriptionRepository.findAllByMemberId(memberId)
        val techBlogIds = subscriptions.map { it.techBlogId }
        if (techBlogIds.isEmpty()) return emptyList()

        return techBlogRepository.findAllById(techBlogIds).map(::TechBlogData).toList()
    }
}
