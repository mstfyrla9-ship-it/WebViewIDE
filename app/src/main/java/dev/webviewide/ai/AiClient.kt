package dev.webviewide.ai

import dev.webviewide.project.encodeJson
import dev.webviewide.project.jsonObject
import dev.webviewide.project.parseJson
import java.net.HttpURLConnection
import java.net.URL

class AiException(message: String) : IllegalStateException(message)

object AiClient {
    fun complete(
        provider: AiProvider,
        baseUrl: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        if (apiKey.isBlank()) throw AiException("先填写 ${provider.title} 的 API Key")
        return when (provider) {
            AiProvider.ANTHROPIC -> completeAnthropic(baseUrl, model, apiKey, systemPrompt, userPrompt)
            AiProvider.GEMINI -> completeGemini(baseUrl, model, apiKey, systemPrompt, userPrompt)
            else -> completeOpenAiCompatible(provider, baseUrl, model, apiKey, systemPrompt, userPrompt)
        }
    }

    private fun completeOpenAiCompatible(
        provider: AiProvider,
        baseUrl: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val endpoint = "${baseUrlFor(provider, baseUrl).trimEnd('/')}/chat/completions"
        val body = mapOf(
            "model" to model,
            "messages" to listOf(
                mapOf("role" to "system", "content" to systemPrompt),
                mapOf("role" to "user", "content" to userPrompt),
            ),
            "temperature" to 0.2,
        )
        val json = post(endpoint, encodeJson(body), mapOf(
            "Authorization" to "Bearer $apiKey",
        ))
        val choices = json["choices"] as? List<*> ?: throw AiException("响应里没有 choices")
        val message = (choices.firstOrNull() as? Map<*, *>)?.get("message") as? Map<*, *>
        val content = message?.get("content") as? String
        return content?.trim().takeUnless { it.isNullOrBlank() } ?: throw AiException("模型没有返回文本")
    }

    private fun completeAnthropic(
        baseUrl: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val endpoint = "${baseUrlFor(AiProvider.ANTHROPIC, baseUrl).trimEnd('/')}/messages"
        val body = mapOf(
            "model" to model,
            "system" to systemPrompt,
            "messages" to listOf(mapOf("role" to "user", "content" to userPrompt)),
            "max_tokens" to 4096,
        )
        val json = post(endpoint, encodeJson(body), mapOf(
            "x-api-key" to apiKey,
            "anthropic-version" to "2023-06-01",
        ))
        val content = json["content"] as? List<*> ?: throw AiException("Anthropic 响应里没有 content")
        val text = content.mapNotNull { block ->
            (block as? Map<*, *>)?.get("text") as? String
        }.joinToString("\n").trim()
        return text.takeIf { it.isNotBlank() } ?: throw AiException("模型没有返回文本")
    }

    private fun completeGemini(
        baseUrl: String,
        model: String,
        apiKey: String,
        systemPrompt: String,
        userPrompt: String,
    ): String {
        val base = baseUrlFor(AiProvider.GEMINI, baseUrl).trimEnd('/')
        val endpoint = "$base/models/$model:generateContent?key=${java.net.URLEncoder.encode(apiKey, "UTF-8")}"
        val body = mapOf(
            "system_instruction" to mapOf(
                "parts" to listOf(mapOf("text" to systemPrompt)),
            ),
            "contents" to listOf(mapOf(
                "role" to "user",
                "parts" to listOf(mapOf("text" to userPrompt)),
            )),
        )
        val json = post(endpoint, encodeJson(body), emptyMap())
        val candidates = json["candidates"] as? List<*> ?: throw AiException("Gemini 响应里没有 candidates")
        val content = (candidates.firstOrNull() as? Map<*, *>)?.get("content") as? Map<*, *>
        val parts = content?.get("parts") as? List<*> ?: throw AiException("Gemini 响应里没有文本")
        val text = parts.mapNotNull { part -> (part as? Map<*, *>)?.get("text") as? String }
            .joinToString("\n").trim()
        return text.takeIf { it.isNotBlank() } ?: throw AiException("模型没有返回文本")
    }

    private fun baseUrlFor(provider: AiProvider, custom: String): String =
        custom.trim().trimEnd('/').ifBlank { provider.defaultBaseUrl }

    @Suppress("UNCHECKED_CAST")
    private fun post(endpoint: String, body: String, headers: Map<String, String>): Map<String, Any?> {
        val connection = (URL(endpoint).openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 30_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
            headers.forEach { (key, value) -> setRequestProperty(key, value) }
        }
        try {
            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..299) connection.inputStream else connection.errorStream
            val text = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            if (code !in 200..299) {
                val message = runCatching {
                    val root = parseJson(text).jsonObject()
                    val error = root["error"]
                    when (error) {
                        is String -> error
                        is Map<*, *> -> error["message"] as? String
                        else -> null
                    } ?: text.take(500)
                }.getOrDefault(text.take(500))
                throw AiException("AI 请求失败（HTTP $code）：${message.ifBlank { "未知错误" }}")
            }
            return runCatching { parseJson(text).jsonObject() }
                .getOrElse { throw AiException("AI 返回的不是有效 JSON") }
        } finally {
            connection.disconnect()
        }
    }
}
