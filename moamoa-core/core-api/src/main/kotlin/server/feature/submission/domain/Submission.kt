package server.feature.submission.domain

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table(name = "submission")
data class Submission(
    @Id
    @Column("id")
    override val id: Long = 0,

    @Column("blog_title")
    val blogTitle: String,

    @Column("blog_url")
    val blogUrl: String,

    @Column("notification_enabled")
    val notificationEnabled: Boolean,

    @Column("accepted")
    val accepted: Boolean,

    @Column("member_id")
    val memberId: Long
) : BaseEntity()
