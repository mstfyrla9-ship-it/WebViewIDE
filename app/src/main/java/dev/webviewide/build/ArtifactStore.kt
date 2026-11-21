package dev.webviewide.build

import android.content.Context
import dev.webviewide.project.encodeJson
import dev.webviewide.project.int
import dev.webviewide.project.parseJson
import dev.webviewide.project.string
import java.io.File

/**
 * Build outputs live in filesDir/builds (never cacheDir, which the system
 * may purge). Metadata is persisted so the list survives restarts.
 */
object ArtifactStore {
    private const val DIR = "builds"
    private const val INDEX = "artifacts.json"

    fun dir(context: Context): File = File(context.filesDir, DIR).apply { mkdirs() }

    fun list(context: Context): List<BuildArtifact> {
        val index = File(dir(context), INDEX)
        if (!index.isFile) return emptyList()
        val rows = runCatching {
            @Suppress("UNCHECKED_CAST")
            (parseJson(index.readText()) as? List<Map<String, Any?>>).orEmpty()
        }.getOrNull() ?: return emptyList()
        return rows.mapNotNull { m ->
            val file = File(dir(context), m.string("file"))
            if (!file.isFile) return@mapNotNull null
            runCatching {
                BuildArtifact(
                    id = m.string("id"),
                    appName = m.string("appName"),
                    versionName = m.string("versionName"),
                    versionCode = m.int("versionCode", 1),
                    kind = runCatching { ArtifactKind.valueOf(m.string("kind")) }.getOrDefault(ArtifactKind.APK_DEBUG),
                    sizeBytes = file.length(),
                    createdAt = (m["createdAt"] as? Number)?.toLong() ?: file.lastModified(),
                    file = file,
                    packageName = m.string("packageName"),
                    minSdk = (m["minSdk"] as? Number)?.toInt(),
                    targetSdk = (m["targetSdk"] as? Number)?.toInt(),
                    architectures = (m["architectures"] as? List<*>)?.mapNotNull { it as? String }?.toSet().orEmpty(),
                    signed = m["signed"] as? Boolean ?: false,
                    aligned = m["aligned"] as? Boolean ?: false,
                )
            }.getOrNull()
        }.sortedByDescending { it.createdAt }
    }

    fun record(context: Context, artifact: BuildArtifact) {
        val rows = list(context).filter { it.id != artifact.id } + artifact
        write(context, rows)
    }

    fun remove(context: Context, id: String) {
        val rows = list(context)
        rows.firstOrNull { it.id == id }?.file?.delete()
        write(context, rows.filter { it.id != id })
    }

    fun exportTo(context: Context, id: String, dest: File): Boolean {
        val artifact = list(context).firstOrNull { it.id == id } ?: return false
        artifact.file.copyTo(dest, overwrite = true)
        return true
    }

    private fun write(context: Context, rows: List<BuildArtifact>) {
        val list = rows.map {
            mapOf(
                "id" to it.id,
                "appName" to it.appName,
                "versionName" to it.versionName,
                "versionCode" to it.versionCode,
                "kind" to it.kind.name,
                "createdAt" to it.createdAt,
                "file" to it.file.name,
                "packageName" to it.packageName,
                "minSdk" to (it.minSdk ?: -1),
                "targetSdk" to (it.targetSdk ?: -1),
                "architectures" to it.architectures.toList(),
                "signed" to it.signed,
                "aligned" to it.aligned,
            )
        }
        File(dir(context), INDEX).writeText(encodeJson(list))
    }
}