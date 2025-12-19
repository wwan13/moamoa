package server.domain.category

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.Index
import jakarta.persistence.Table
import support.domain.BaseEntity

@Entity
@Table(
    name = "category",
    indexes = [
        Index(name = "idx_category_title", columnList = "title", unique = true)
    ]
)
data class Category(
    @Column(name = "title", nullable = false, unique = true)
    val title: String
) : BaseEntity()