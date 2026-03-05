package server.ai

interface ChatCompletion {
    suspend fun invoke(
        vararg prompts: Prompt,
        temperature: Double = 0.0
    ): String

    suspend fun invoke(
        prompts: List<Prompt>,
        temperature: Double = 0.0
    ): String
}
