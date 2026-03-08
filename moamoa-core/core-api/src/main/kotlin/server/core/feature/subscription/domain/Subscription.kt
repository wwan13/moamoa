package server.core.feature.subscription.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import server.core.support.domain.BaseEntity

@Entity
@Table(
    name = "subscription",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_tech_blog", columnNames = ["member_id", "tech_blog_id"])],
    indexes = [
        Index(name = "idx_member_id", columnList = "member_id"),
        Index(name = "idx_tech_blog_id", columnList = "tech_blog_id")
    ]
)
class Subscription(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    notificationEnabled: Boolean,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "tech_blog_id", nullable = false)
    val techBlogId: Long
) : BaseEntity() {
    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = notificationEnabled
        private set


    fun subscribe() {
        registerEvent(
            TechBlogSubscribeUpdatedEvent(
                memberId = memberId,
                techBlogId = techBlogId,
                subscribed = true
            )
        )
    }

    fun unsubscribe() {
        registerEvent(
            TechBlogSubscribeUpdatedEvent(
                memberId = memberId,
                techBlogId = techBlogId,
                subscribed = false
            )
        )
    }

    fun toggleNotification() {
        notificationEnabled = !notificationEnabled
        registerEvent(
            NotificationUpdatedEvent(
                memberId = memberId,
                techBlogId = techBlogId,
                enabled = notificationEnabled
            )
        )
    }
}
