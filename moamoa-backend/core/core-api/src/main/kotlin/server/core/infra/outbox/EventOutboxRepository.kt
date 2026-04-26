package server.core.infra.outbox

import org.springframework.data.jpa.repository.Modifying
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        UPDATE EventOutbox e
        SET e.published = true
        WHERE e.id IN :ids
          AND e.published = false
    """
    )
    fun markPublishedByIds(ids: List<Long>): Int
}
