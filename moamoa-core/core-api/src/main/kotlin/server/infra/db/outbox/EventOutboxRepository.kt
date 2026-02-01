package server.infra.db.outbox

import org.springframework.data.r2dbc.repository.Modifying
import org.springframework.data.r2dbc.repository.Query
import org.springframework.data.repository.kotlin.CoroutineCrudRepository

interface EventOutboxRepository : CoroutineCrudRepository<EventOutbox, Long> {

    @Query("""
        SELECT * 
        FROM event_outbox
        WHERE published = false
        ORDER BY created_at
        LIMIT :limit
    """)
    suspend fun findUnpublished(limit: Int): List<EventOutbox>

    @Modifying
    @Query("""
    UPDATE event_outbox
    SET published = true
    WHERE id = :id AND published = false
""")
    suspend fun markPublished(id: Long): Int?
}