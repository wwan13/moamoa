package server.infra.db.outbox

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import support.domain.BaseEntity

@Entity
@Table(
    name = "event_outbox",
    indexes = [Index(name = "idx_outbox_published_created", columnList = "published,created_at")]
)
class EventOutbox(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    override val id: Long = 0,

    @Column(name = "topic", length = 255, nullable = false)
    val topic: String,

    @Column(name = "type", length = 256, nullable = false)
    val type: String,

    @Column(name = "payload", columnDefinition = "json", nullable = false)
    val payload: String,

    published: Boolean = false,
) : BaseEntity() {
    @Column(name = "published", nullable = false)
    var published: Boolean = published
        private set

    fun markPublished() {
        published = true
    }
}
