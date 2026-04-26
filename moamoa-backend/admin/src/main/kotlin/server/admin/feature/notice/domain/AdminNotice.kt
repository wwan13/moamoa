package server.admin.feature.notice.domain

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import server.admin.support.domain.AdminBaseEntity
import java.time.LocalDateTime

@Entity
@Table(name = "notice")
internal class AdminNotice(
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
    var published: Boolean,

    @Column(name = "published_at")
    val publishedAt: LocalDateTime,
) : AdminBaseEntity() {
    fun updatePublished(published: Boolean) {
        this.published = published
    }
}
