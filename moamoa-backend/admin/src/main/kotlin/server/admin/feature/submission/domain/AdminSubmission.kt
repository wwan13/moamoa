package server.admin.feature.submission.domain

import jakarta.persistence.Entity
import jakarta.persistence.Column
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(name = "submission")
internal class AdminSubmission(
    @Id
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "blog_title")
    val blogTitle: String,

    @Column(name = "blog_url")
    val blogUrl: String,

    @Column(name = "notification_enabled")
    val notificationEnabled: Boolean,

    @Column(name = "accepted")
    val accepted: Boolean,

    @Column(name = "member_id")
    val memberId: Long
) : AdminBaseEntity()
