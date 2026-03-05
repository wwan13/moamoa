package server.core.infra.db.outbox

import org.springframework.data.jpa.repository.Query
import org.springframework.data.jpa.repository.JpaRepository

interface EventOutboxRepository : JpaRepository<EventOutbox, Long> {

    @Query(
        value = """
        SELECT * 
        FROM event_outbox
        WHERE published = false
        ORDER BY created_at
        LIMIT :limit
    """,
        nativeQuery = true
    )
    fun findUnpublished(limit: Int): List<EventOutbox>
}
