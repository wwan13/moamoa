package server.admin.feature.member.domain

internal enum class AdminProvider {
    INTERNAL, GOOGLE, GITHUB;

    companion object {
        fun from(providerName: String): AdminProvider =
            entries.firstOrNull { it.name.equals(providerName, ignoreCase = true) }
                ?: throw IllegalStateException("Provider $providerName not found")
    }
}