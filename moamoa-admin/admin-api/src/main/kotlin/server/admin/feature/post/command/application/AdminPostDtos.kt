package server.admin.feature.post.command.application

internal data class AdminUpdateCategoryCommand(
    val categoryId: Long
)

internal data class AdminUpdateCategoryResult(
    val success: Boolean
)