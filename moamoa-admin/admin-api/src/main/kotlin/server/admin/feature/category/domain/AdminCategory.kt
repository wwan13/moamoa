package server.admin.feature.category.domain

internal enum class AdminCategory(
    val id: Long,
    val title: String
) {
    BACKEND(1L, "백앤드"),
    FRONTEND(2L, "프론트앤드"),
    INFRA(3L, "인프라"),
    DATA_ML_AI(4L, "데이터·ML·AI"),
    DESIGN_PRODUCT(5L, "디자인·프로덕트"),
    ETC(6L, "기타"),

    UNDEFINED(999L, "");

    companion object {
        val validCategories: List<AdminCategory>
            get() = AdminCategory.entries.filter { it != UNDEFINED }

        fun fromId(id: Long) = validCategories.firstOrNull { it.id == id }
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")

        fun fromName(name: String) = validCategories.firstOrNull { it.name == name }
            ?: throw IllegalArgumentException("존재하지 않는 카테고리입니다.")
    }
}
