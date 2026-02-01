package server.feature.techblogsubscription.application

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
import server.global.lock.KeyedMutex
import server.infra.db.transaction.Transactional

@Service
class TechBlogSubscriptionService(
    private val transactional: Transactional,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val keyedMutex: KeyedMutex
) {

    suspend fun toggle(
        command: TechBlogSubscriptionToggleCommand,
        memberId: Long
    ): TechBlogSubscriptionToggleResult {
        val mutexKey = "techBlogSubscriptionToggle:$memberId:${command.techBlogId}"
        return keyedMutex.withLock(mutexKey) {
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
        return keyedMutex.withLock(mutexKey) {
            transactional {
                val subscription = techBlogSubscriptionRepository
                    .findByMemberIdAndTechBlogId(memberId, command.techBlogId)
                    ?: throw IllegalArgumentException("구독중이지 않은 기술 블로그 입니다.")

                val updated = subscription.toggleNotification()
                techBlogSubscriptionRepository.save(updated.entity)
                registerEvent(updated.event)

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