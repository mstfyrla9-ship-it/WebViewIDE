package dev.webviewide.ai

enum class AiProvider(val title: String, val defaultBaseUrl: String, val defaultModel: String) {
    OPENAI("OpenAI", "https://api.openai.com/v1", "gpt-4o-mini"),
    ANTHROPIC("Anthropic", "https://api.anthropic.com/v1", "claude-sonnet-4-5"),
    GEMINI("Google Gemini", "https://generativelanguage.googleapis.com/v1beta", "gemini-2.5-flash"),
    OPENROUTER("OpenRouter", "https://openrouter.ai/api/v1", "openai/gpt-4o-mini"),
    GROQ("Groq", "https://api.groq.com/openai/v1", "llama-3.3-70b-versatile"),
    DEEPSEEK("DeepSeek", "https://api.deepseek.com", "deepseek-chat"),
    XAI("xAI", "https://api.x.ai/v1", "grok-3-mini"),
    MISTRAL("Mistral", "https://api.mistral.ai/v1", "mistral-small-latest"),
    CUSTOM("موافق لـ OpenAI", "", "gpt-4o-mini");

    companion object {
        fun fromId(id: String?): AiProvider =
            entries.firstOrNull { it.name == id } ?: OPENAI
    }
}
