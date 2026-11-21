package dev.webviewide.ai

import android.content.Context
import dev.webviewide.build.KeystoreManager
import dev.webviewide.project.encodeJson
import dev.webviewide.project.parseJson
import java.io.File

data class AiConfig(
    val providerId: String = AiProvider.OPENAI.name,
    val model: String = AiProvider.OPENAI.defaultModel,
    val baseUrl: String = "",
)

object AiConfigStore {
    private const val INDEX = "ai-config.json"
    private const val KEYS = "ai-keys.json"

    fun load(context: Context): AiConfig {
        val file = File(context.filesDir, INDEX)
        val root = runCatching {
            @Suppress("UNCHECKED_CAST")
            (parseJson(file.readText()) as? Map<String, Any?>).orEmpty()
        }.getOrElse { emptyMap() }
        val provider = AiProvider.fromId(root["provider"] as? String)
        val model = (root["model"] as? String)?.ifBlank { provider.defaultModel } ?: provider.defaultModel
        val baseUrl = (root["baseUrl"] as? String)?.trim().orEmpty()
        return AiConfig(provider.name, model, baseUrl)
    }

    fun save(context: Context, config: AiConfig) {
        val provider = AiProvider.fromId(config.providerId)
        val model = config.model.trim().ifBlank { provider.defaultModel }
        val baseUrl = config.baseUrl.trim().trimEnd('/')
        File(context.filesDir, INDEX).writeText(encodeJson(mapOf(
            "provider" to provider.name,
            "model" to model,
            "baseUrl" to baseUrl,
        )))
    }

    fun saveKey(context: Context, provider: AiProvider, apiKey: String) {
        val root = readKeys(context).toMutableMap()
        if (apiKey.isBlank()) root.remove(provider.name)
        else root[provider.name] = KeystoreManager.wrap(apiKey.trim())
        writeKeys(context, root)
    }

    fun loadKey(context: Context, provider: AiProvider): String {
        val wrapped = readKeys(context)[provider.name] as? String ?: return ""
        return runCatching { KeystoreManager.unwrap(wrapped) }.getOrDefault("")
    }

    private fun readKeys(context: Context): Map<String, String> {
        val file = File(context.filesDir, KEYS)
        if (!file.isFile) return emptyMap()
        val root = runCatching {
            @Suppress("UNCHECKED_CAST")
            (parseJson(file.readText()) as? Map<String, Any?>).orEmpty()
        }.getOrElse { emptyMap() }
        return root.mapNotNull { (key, value) -> (value as? String)?.let { key to it } }.toMap()
    }

    private fun writeKeys(context: Context, keys: Map<String, String>) {
        File(context.filesDir, KEYS).writeText(encodeJson(keys))
    }
}
