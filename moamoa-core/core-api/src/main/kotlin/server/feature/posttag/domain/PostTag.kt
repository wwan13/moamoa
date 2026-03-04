package server.feature.posttag.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import support.domain.BaseEntity

@Entity
@Table(
    name = "post_tag",
    uniqueConstraints = [UniqueConstraint(name = "uk_post_tag_post_id_tag_id", columnNames = ["post_id", "tag_id"])],
    indexes = [
        Index(name = "FK_POST_TAG_ON_POST", columnList = "post_id"),
        Index(name = "FK_POST_TAG_ON_TAG", columnList = "tag_id")
    ]
)
class PostTag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "post_id", nullable = false)
    val postId: Long,

    @Column(name = "tag_id", nullable = false)
    val tagId: Long,
) : BaseEntity()
