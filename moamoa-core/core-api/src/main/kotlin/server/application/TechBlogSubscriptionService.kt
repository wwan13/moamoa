package server.application

import org.springframework.stereotype.Service
import server.domain.member.MemberRepository
import server.domain.techblog.TechBlogRepository
import server.domain.techblogsubscription.TechBlogSubscription
import server.domain.techblogsubscription.TechBlogSubscriptionRepository

@Service
class TechBlogSubscriptionService(
    private val techBlogSubscriptionRepository: TechBlogSubscriptionRepository,
    private val techBlogRepository: TechBlogRepository,
    private val memberRepository: MemberRepository
) {

    suspend fun toggle(
        command: TechBlogSubscriptionToggleCommand,
        memberId: Long
    ): TechBlogSubscriptionToggleResult {
        if (!memberRepository.existsById(memberId)) {
            throw IllegalArgumentException("존재하지 않는 사용자 입니다.")
        }
        if (!techBlogRepository.existsById(command.techBlogId)) {
            throw IllegalArgumentException("존재하지 않는 기술 블로그 입니다.")
        }

        val subscribing = techBlogSubscriptionRepository.findByMemberIdAndTechBlogId(memberId, command.techBlogId)
            ?.let { subscription ->
                techBlogSubscriptionRepository.deleteById(subscription.id)
                false
            }
            ?: let {
                val subscription = TechBlogSubscription(
                    notificationEnabled = true,
                    memberId = memberId,
                    techBlogId = command.techBlogId
                )
                techBlogSubscriptionRepository.save(subscription)
                true
            }

        return TechBlogSubscriptionToggleResult(subscribing)
    }
}