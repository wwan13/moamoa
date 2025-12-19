package server.admin.domain.category

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import support.admin.domain.BaseEntity

@Entity
@Table(
    name = "category",
    indexes = [
        Index(name = "idx_category_title", columnList = "title", unique = true)
    ]
)
data class AdminCategory(
    @Column(name = "title", nullable = false, unique = true)
    val title: String
) : BaseEntity()