package server.feature.techblogsubscription.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity
import support.domain.DomainModified

@Table("tech_blog_subscription")
data class TechBlogSubscription(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("notification_enabled")
    val notificationEnabled: Boolean,

    @Column("member_id")
    val memberId: Long,

    @Column("tech_blog_id")
    val techBlogId: Long
) : BaseEntity() {

    fun subscribe() = TechBlogSubscribeUpdatedEvent(
        memberId = memberId,
        techBlogId = techBlogId,
        subscribed = true
    )

    fun unsubscribe() = TechBlogSubscribeUpdatedEvent(
        memberId = memberId,
        techBlogId = techBlogId,
        subscribed = false
    )

    fun toggleNotification(): DomainModified<TechBlogSubscription> {
        val toggled = copy(
            notificationEnabled = !notificationEnabled,
        )
        val event = NotificationUpdatedEvent(
            memberId = memberId,
            techBlogId = techBlogId,
            enabled = toggled.notificationEnabled
        )

        return DomainModified(toggled, event)
    }
}