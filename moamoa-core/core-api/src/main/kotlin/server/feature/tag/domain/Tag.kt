package server.feature.tag.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import support.domain.BaseEntity

@Entity
@Table(
    name = "tag",
    uniqueConstraints = [UniqueConstraint(name = "idx_tag_title", columnNames = ["title"])]
)
class Tag(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "title", length = 255, nullable = false)
    val title: String,
) : BaseEntity()
