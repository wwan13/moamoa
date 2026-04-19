package server.core.feature.member.domain

enum class MemberProvider {
    INTERNAL, GOOGLE, GITHUB;

    companion object {
        fun from(providerName: String): MemberProvider =
            entries.firstOrNull { it.name.equals(providerName, ignoreCase = true) }
                ?: throw IllegalStateException("Provider $providerName not found")
    }
}
