package server.admin.feature.bookmark.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import server.admin.support.domain.AdminBaseEntity

@Entity
@Table(
    name = "bookmark",
    uniqueConstraints = [UniqueConstraint(name = "uk_member_post", columnNames = ["member_id", "post_id"])],
    indexes = [Index(name = "idx_post_id", columnList = "post_id")],
)
internal class AdminBookmark(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "member_id", nullable = false)
    val memberId: Long,

    @Column(name = "post_id", nullable = false)
    val postId: Long,
) : AdminBaseEntity()
