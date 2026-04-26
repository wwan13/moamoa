package server.core.feature.notice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.core.support.domain.BaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "notice")
class Notice(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    override val id: Long = 0,

    @Column(name = "title", length = 2048)
    val title: String,

    @Column(name = "chip", length = 256)
    val chip: String,

    @Column(name = "content", columnDefinition = "TEXT")
    val content: String,

    @Column(name = "published")
    val published: Boolean,

    @Column(name = "published_at")
    val publishedAt: LocalDateTime,
) : BaseEntity()
