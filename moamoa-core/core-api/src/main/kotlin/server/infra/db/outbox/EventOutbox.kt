package server.infra.db.outbox

import org.springframework.data.annotation.Id
import org.springframework.data.relational.core.mapping.Column
import org.springframework.data.relational.core.mapping.Table
import support.domain.BaseEntity

@Table("event_outbox")
data class EventOutbox(
    @Id
    @Column("id")
    override val id: Long = 0,

    val topic: String,

    val type: String,

    val payload: String,

    val published: Boolean = false,
) : BaseEntity()
