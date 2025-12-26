package server.domain.techblogsubscription

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

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
) : BaseEntity()