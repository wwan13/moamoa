package server.batch.member.generatealarmcontent.processor

import org.springframework.stereotype.Component
import server.batch.member.generatealarmcontent.dto.AlarmContent
import server.batch.member.generatealarmcontent.dto.MemberData
import server.batch.member.generatealarmcontent.lookup.MemberSubscriptionLookup
import server.batch.member.generatealarmcontent.lookup.NewPostsLookup

@Component
internal class GenerateAlarmContentProcessor(
    private val memberSubscriptionLookup: MemberSubscriptionLookup,
    private val newPostsLookup: NewPostsLookup
) {

    fun process(
        item: MemberData,
        subscribingBlogIdsByMemberId: Map<Long, List<Long>>,
        postsByTechBlogId: Map<Long, List<server.batch.member.generatealarmcontent.dto.PostData>>,
    ): AlarmContent? {

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

    suspend fun loadSubscriptions(): Map<Long, List<Long>> = memberSubscriptionLookup.load()

    suspend fun loadNewPosts(): Map<Long, List<server.batch.member.generatealarmcontent.dto.PostData>> = newPostsLookup.load()
}
