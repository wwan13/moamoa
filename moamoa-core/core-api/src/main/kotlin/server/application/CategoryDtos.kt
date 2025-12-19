package server.application

import server.domain.category.Category

data class CategoryData(
    val id: Long,
    val title: String
) {
    constructor(category: Category) : this(
        id = category.id,
        title = category.title
    )
}