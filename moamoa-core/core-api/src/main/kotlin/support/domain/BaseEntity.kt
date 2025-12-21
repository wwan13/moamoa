package support.domain

import org.springframework.data.annotation.CreatedDate
import org.springframework.data.annotation.LastModifiedDate
import org.springframework.data.domain.Persistable
import org.springframework.data.relational.core.mapping.Column
import java.time.LocalDateTime

abstract class BaseEntity : Persistable<Long> {
    abstract val id: Long

    @CreatedDate
    @Column("created_at")
    var createdAt: LocalDateTime = LocalDateTime.now()

    @LastModifiedDate
    @Column("last_modified_at")
    var lastModifiedAt: LocalDateTime = LocalDateTime.now()

    override fun getId(): Long = id

    override fun isNew(): Boolean = id == 0L
}