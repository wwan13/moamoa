package server.feature.submission.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import support.domain.BaseEntity

@Entity
@Table(name = "submission")
class Submission(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "blog_title", length = 255, nullable = false)
    val blogTitle: String,

    @Column(name = "blog_url", length = 255, nullable = false)
    val blogUrl: String,

    @Column(name = "notification_enabled", nullable = false)
    val notificationEnabled: Boolean,

    @Column(name = "accepted", nullable = false)
    val accepted: Boolean,

    @Column(name = "member_id", nullable = false)
    val memberId: Long
) : BaseEntity() {

    fun created() = SubmissionCreateEvent(
        submissionId = id,
        blogTitle = blogTitle,
        blogUrl = blogUrl
    )
}
