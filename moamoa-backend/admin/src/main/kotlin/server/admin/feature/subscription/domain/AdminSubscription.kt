package server.admin.feature.subscription.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "subscription")
internal class AdminSubscription(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "tech_blog_id", nullable = false)
    val techBlogId: Long,

    notificationEnabled: Boolean,
) : AdminBaseEntity() {
    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = notificationEnabled
        private set
}
