package server.admin.domain.postcategory

import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import server.admin.domain.category.AdminCategory
import server.admin.domain.post.AdminPost
import support.admin.domain.BaseEntity

@Entity
@Table(name = "post_category")
data class AdminPostCategory(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    val post: AdminPost,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    val category: AdminCategory,
) : BaseEntity()