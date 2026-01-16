package server.feature.member.domain

enum class Provider {
    INTERNAL, GOOGLE, GITHUB;

    companion object {
        fun from(providerName: String): Provider =
            entries.firstOrNull { it.name.equals(providerName, ignoreCase = true) }
                ?: throw IllegalStateException("Provider $providerName not found")
    }
}