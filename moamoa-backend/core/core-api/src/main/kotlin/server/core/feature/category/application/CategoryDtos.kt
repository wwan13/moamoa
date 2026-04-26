package server.core.feature.category.application

import server.core.feature.category.domain.Category

data class CategoryData(
    val id: Long,
    val title: String,
    val key: String
) {
    constructor(category: Category) : this(
        id = category.id,
        title = category.title,
        key = category.name
    )
}