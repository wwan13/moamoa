package server.feature.techblogsubscription.application

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import org.springframework.stereotype.Service
import server.feature.member.domain.MemberRepository
import server.feature.techblog.command.application.TechBlogData
import server.feature.techblog.command.domain.TechBlogRepository
import server.feature.techblogsubscription.domain.*
import server.infra.db.Transactional
import server.messaging.StreamEventPublisher
import server.messaging.StreamTopic

@Service
class TechBlogSubscriptionService(
    private val transactional: Transactional,
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository,
    private val eventPublisher: StreamEventPublisher,
    private val defaultTopic: StreamTopic
) {

    suspend fun toggle(
        command: TechBlogSubscriptionToggleCommand,
        memberId: Long
    ): TechBlogSubscriptionToggleResult = transactional {
        if (!memberRepository.existsById(memberId)) {
            throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
        }
        if (!techBlogRepository.existsById(command.techBlogId)) {
            throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
        }

        val subscribing = techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?.let { subscription ->
                techBlogSubscriptionRepository.deleteById(subscription.id)
                val event = TechBlogSubscribeRemovedEvent(memberId, command.techBlogId)
                eventPublisher.publish(defaultTopic, event)
                false
            }
            ?: let {
                val subscription = TechBlogSubscription(
                    notificationEnabled = true,
                    memberId = memberId,
                    techBlogId = command.techBlogId
                )
                techBlogSubscriptionRepository.save(subscription)
                val event = TechBlogSubscribeCreatedEvent(memberId, command.techBlogId)
                eventPublisher.publish(defaultTopic, event)
                true
            }

        TechBlogSubscriptionToggleResult(subscribing)
    }

    suspend fun notificationEnabledToggle(
        command: NotificationEnabledToggleCommand,
        memberId: Long
    ): NotificationEnabledToggleResult = transactional {
        val subscription = techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?: throw IllegalArgumentException("구독중이지 않은 기술 블로그 입니다.")

        val updated = subscription.copy(
            notificationEnabled = !subscription.notificationEnabled
        )
        techBlogSubscriptionRepository.save(updated)

        val event = if (updated.notificationEnabled) {
            NotificationEnabledEvent(memberId, command.techBlogId)
        } else {
            NotificationDisabledEvent(memberId, command.techBlogId)
        }
        eventPublisher.publish(defaultTopic, event)

        NotificationEnabledToggleResult(updated.notificationEnabled)
    }

    suspend fun subscribingTechBlogs(memberId: Long): Flow<TechBlogData> {
        val subscriptions = techBlogSubscriptionRepository.findAllByMemberId(memberId).toList()
        val techBlogIds = subscriptions.map { it.techBlogId }
        if (techBlogIds.isEmpty()) return emptyFlow()

        return techBlogRepository.findAllById(techBlogIds).map(::TechBlogData)
    }
}