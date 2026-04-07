package server.batch.member.processor

import org.springframework.batch.item.ItemProcessor
import org.springframework.stereotype.Component
import server.batch.member.dto.AlarmContent
import server.batch.member.dto.MemberData
import server.batch.member.lookup.MemberSubscriptionLookup
import server.batch.member.lookup.NewPostsLookup

@Component
internal class GenerateAlarmContentProcessor(
    private val memberSubscriptionLookup: MemberSubscriptionLookup,
    private val newPostsLookup: NewPostsLookup
) : ItemProcessor<MemberData, AlarmContent> {

    override fun process(item: MemberData): AlarmContent? {
        val subscribingBlogIdsByMemberId = memberSubscriptionLookup.subscribingBlogIdsByMemberId
        val postsByTechBlogId = newPostsLookup.postsByTechBlogId

        val subscribingTechBlogIds = subscribingBlogIdsByMemberId[item.memberId]?.toSet()
            ?: return null
        val techBlogIds = postsByTechBlogId.keys

        val intersection = subscribingTechBlogIds intersect techBlogIds
        if (intersection.isEmpty()) {
            return null
        }

        val techBlogs = intersection.map {
            val posts = postsByTechBlogId[it] ?: return null
            val firstPost = posts.firstOrNull() ?: return null

            AlarmContent.TechBlog(
                techBlogId = firstPost.techBlogId,
                techBlogTitle = firstPost.techBlogTitle,
                techBlogIcon = firstPost.techBlogIcon,
                posts = posts.map {
                    AlarmContent.TechBlog.Post(
                        postId = it.postId,
                        postTitle = it.postTitle,
                        postDescription = it.postDescription,
                        postThumbnail = it.postThumbnail,
                        postUrl = it.postUrl
                    )
                }
            )
        }

        return AlarmContent(
            memberId = item.memberId,
            email = item.email,
            techBlog = techBlogs
        )
    }
}