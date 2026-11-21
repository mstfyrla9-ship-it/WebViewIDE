package dev.webviewide.build

import com.reandroid.apk.ApkModule
import java.io.File
import java.util.zip.ZipFile

data class ApkInfo(
    val packageName: String?,
    val versionName: String?,
    val versionCode: Int?,
    val minSdk: Int?,
    val targetSdk: Int?,
    val architectures: Set<String>,
    val sizeBytes: Long,
    val signed: Boolean,
    val aligned: Boolean,
)

object ApkInspector {
    fun inspect(file: File): ApkInfo {
        require(file.isFile && file.extension.equals("apk", ignoreCase = true)) {
            "Not an APK file"
        }
        val module = runCatching { ApkModule.loadApkFile(file) }.getOrNull()
        val manifest = module?.androidManifest
        val packageName = manifest?.packageName
        val versionName = manifest?.versionName
        val versionCode = manifest?.versionCode
        val architectures = mutableSetOf<String>()
        ZipFile(file).use { zip ->
            zip.entries().asSequence().forEach { entry ->
                val name = entry.name
                val abi = name.substringAfter("lib/", "").substringBefore('/', "")
                if (abi.isNotBlank()) architectures += abi
            }
        }
        module?.close()
        val signed = runCatching {
            ApkSignatureVerifier.verifyOrThrow(file)
            true
        }.getOrDefault(false)
        val aligned = runCatching { ZipAlign.verify(file) }.getOrDefault(false)
        return ApkInfo(
            packageName = packageName,
            versionName = versionName,
            versionCode = versionCode,
            minSdk = null,
            targetSdk = null,
            architectures = architectures,
            sizeBytes = file.length(),
            signed = signed,
            aligned = aligned,
        )
    }
}
