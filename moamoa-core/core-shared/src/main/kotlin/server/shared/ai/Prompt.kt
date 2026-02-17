package server.shared.ai

data class Prompt(
    val role: String,
    val message: String
) {
    private enum class Role(val value: String) {
        SYSTEM("system"), USER("user"), ASSISTANT("assistant")
    }

    companion object {
        fun system(message: String): Prompt =
            Prompt(Role.SYSTEM.value, message)

        fun user(message: String): Prompt =
            Prompt(Role.USER.value, message)

        fun assistant(message: String): Prompt =
            Prompt(Role.ASSISTANT.value, message)
    }
}
