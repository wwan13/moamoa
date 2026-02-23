package server.feature.techblogsubscription.application

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.feature.member.command.domain.MemberRepository
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.TechBlogSubscription
import server.feature.techblogsubscription.domain.TechBlogSubscriptionRepository
import server.global.logging.infoWithTrace
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

    suspend fun toggle(
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
                        logger.infoWithTrace {
                            "[BIZ] what=techBlogSubscribe result=SUCCESS targetId=${command.techBlogId} reason=구독 해제 userId=$memberId"
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
                        logger.infoWithTrace {
                            "[BIZ] what=techBlogSubscribe result=SUCCESS targetId=${command.techBlogId} reason=구독 등록 userId=$memberId"
                        }

                        TechBlogSubscriptionToggleResult(true)
                    }

            }
        }
    }

    suspend fun notificationEnabledToggle(
        command: NotificationEnabledToggleCommand,
        memberId: Long
    ): NotificationEnabledToggleResult {
        val mutexKey = "notificationEnabledToggle:$memberId:${command.techBlogId}"
        return keyedLock.withLock(mutexKey) {
            transactional {
                val subscription = techBlogSubscriptionRepository
                    .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                    ?: throw IllegalArgumentException("구독중이지 않은 기술 블로그 입니다.")

                val updated = subscription.toggleNotification()
                techBlogSubscriptionRepository.save(updated.entity)
                registerEvent(updated.event)
                logger.infoWithTrace {
                    "[BIZ] what=techBlogNotification result=SUCCESS targetId=${command.techBlogId} reason=알림 토글 userId=$memberId"
                }

                NotificationEnabledToggleResult(updated.entity.notificationEnabled)
            }
        }
    }

    suspend fun subscribingTechBlogs(memberId: Long): Flow<TechBlogData> {
        val subscriptions = techBlogSubscriptionRepository.findAllByMemberId(memberId).toList()
        val techBlogIds = subscriptions.map { it.techBlogId }
        if (techBlogIds.isEmpty()) return emptyFlow()

        return techBlogRepository.findAllById(techBlogIds).map(::TechBlogData)
    }
}
