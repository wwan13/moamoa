package server.chat

sealed class Prompt(
    val role: String,
    val message: String
) {
    private enum class Role(val value: String) {
        SYSTEM("system"), USER("user"), ASSISTANT("assistant")
    }

    class System(message: String) : Prompt(Role.SYSTEM.value, message)
    class User(message: String) : Prompt(Role.USER.value, message)
    class Assistant(message: String) : Prompt(Role.ASSISTANT.value, message)
}