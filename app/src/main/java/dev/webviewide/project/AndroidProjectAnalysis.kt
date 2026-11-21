package dev.webviewide.project

import java.io.File

enum class AndroidProjectType {
    NONE,
    ANDROID_GRADLE,
    JAVA,
    KOTLIN,
    APK,
    AAB,
    WEB,
}

data class AndroidProjectAnalysis(
    val type: AndroidProjectType,
    val packageName: String? = null,
    val versionName: String? = null,
    val versionCode: Int? = null,
    val minSdk: Int? = null,
    val targetSdk: Int? = null,
    val compileSdk: Int? = null,
    val agpVersion: String? = null,
    val gradleVersion: String? = null,
    val languages: Set<String> = emptySet(),
    val dependencies: List<String> = emptyList(),
    val missingFiles: List<String> = emptyList(),
    val issues: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
)

object AndroidProjectAnalysisEngine {
    private val quoted = Regex("[\"']([^\"']+)[\"']")
    private val versionCatalog = Regex("(?i)(?:implementation|api|classpath)\\s*[\\(\"']([^\\)\"']+)")

    fun analyze(root: File): AndroidProjectAnalysis {
        if (!root.isDirectory) return AndroidProjectAnalysis(AndroidProjectType.NONE)

        val apk = root.walkTopDown().firstOrNull { it.isFile && it.extension.equals("apk", true) }
        if (apk != null) return AndroidProjectAnalysis(AndroidProjectType.APK, issues = listOf("APK imported as artifact; source recovery requires a decompiler"))
        val aab = root.walkTopDown().firstOrNull { it.isFile && it.extension.equals("aab", true) }
        if (aab != null) return AndroidProjectAnalysis(AndroidProjectType.AAB, issues = listOf("AAB imported as artifact; APK conversion requires a bundletool-compatible build pipeline"))

        val manifests = root.walkTopDown()
            .onEnter { it == root || !ArchiveImport.shouldSkipDir(it.name) }
            .filter { it.isFile && it.name.equals("AndroidManifest.xml", ignoreCase = true) }
            .toList()
        val gradleFiles = root.walkTopDown()
            .onEnter { it == root || !ArchiveImport.shouldSkipDir(it.name) }
            .filter { it.isFile && (it.name.endsWith(".gradle") || it.name.endsWith(".gradle.kts")) }
            .toList()
        val languages = buildSet {
            if (root.walkTopDown().any { it.isFile && it.extension.equals("java", true) }) add("Java")
            if (root.walkTopDown().any { it.isFile && it.extension.equals("kt", true) }) add("Kotlin")
            if (root.walkTopDown().any { it.isFile && it.extension.equals("xml", true) }) add("XML")
        }

        if (manifests.isNotEmpty() || gradleFiles.any { it.name.contains("build.gradle") }) {
            val manifestsText = manifests.joinToString("\n") { it.readText() }
            val gradleText = gradleFiles.joinToString("\n") { it.readText() }
            val packageName = Regex("""<manifest[^>]*package\s*=\s*"([^"]+)"""")
                .find(manifestsText)?.groupValues?.get(1)
                ?: Regex("""applicationId\s*[=:]\s*["']([^"']+)["']""")
                    .find(gradleText)?.groupValues?.get(1)
            val versionName = Regex("""versionName\s*[=:]\s*["']([^"']+)["']""")
                .find(gradleText)?.groupValues?.get(1)
                ?: Regex("""android:versionName\s*=\s*"([^"]+)"""")
                    .find(manifestsText)?.groupValues?.get(1)
            val versionCode = Regex("""versionCode\s*[=:]\s*(\d+)""")
                .find(gradleText)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""android:versionCode\s*=\s*"(\d+)"""")
                    .find(manifestsText)?.groupValues?.get(1)?.toIntOrNull()
            val minSdk = Regex("""minSdk(?:Version)?(?:\s*[=:]\s*|\s*>\s*)(\d+)""")
                .find(gradleText)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""android:minSdkVersion\s*=\s*"(\d+)"""")
                    .find(manifestsText)?.groupValues?.get(1)?.toIntOrNull()
            val targetSdk = Regex("""targetSdk(?:Version)?(?:\s*[=:]\s*|\s*>\s*)(\d+)""")
                .find(gradleText)?.groupValues?.get(1)?.toIntOrNull()
                ?: Regex("""android:targetSdkVersion\s*=\s*"(\d+)"""")
                    .find(manifestsText)?.groupValues?.get(1)?.toIntOrNull()
            val compileSdk = Regex("""compileSdk(?:Version)?(?:\s*[=:]\s*|\s*>\s*)(\d+)""")
                .find(gradleText)?.groupValues?.get(1)?.toIntOrNull()
            val agp = Regex("""com\.android\.(?:application|library)\s*[:=]\s*["']?([^"'\s]+)""")
                .find(gradleText)?.groupValues?.get(1)
                ?: Regex("""com\.android\.tools\.build:gradle[: ]([0-9][^"'\s]*)""")
                    .find(gradleText)?.groupValues?.get(1)
            val wrapper = File(root, "gradle/wrapper/gradle-wrapper.properties")
            val gradleVersion = if (wrapper.isFile) {
                Regex("""gradle-([0-9][^/-]*)-""").find(wrapper.readText())?.groupValues?.get(1)
            } else null
            val dependencies = versionCatalog.findAll(gradleText)
                .mapNotNull { it.groupValues.getOrNull(1)?.trim() }
                .filter { it.isNotBlank() }
                .distinct()
                .sorted()
                .toList()

            val missing = buildList {
                if (gradleFiles.isEmpty()) add("settings.gradle / build.gradle")
                if (manifests.isEmpty()) add("AndroidManifest.xml")
                if (!File(root, "gradle/wrapper/gradle-wrapper.properties").isFile) add("gradle-wrapper.properties")
            }
            val issues = buildList {
                if (manifests.isEmpty()) add("No AndroidManifest.xml found")
                if (!gradleFiles.any { it.name.startsWith("build.gradle") }) add("No module build.gradle found")
            }
            val warnings = buildList {
                if (compileSdk == null) add("compileSdk is not explicit")
                if (targetSdk == null) add("targetSdk is not explicit")
                if (gradleVersion == null) add("Gradle Wrapper version could not be determined")
                if (agp != null && agp.startsWith("7.") || agp != null && agp.startsWith("8.")) {
                    // Explicit AGP versions are reported, not modified.
                }
            }
            val type = if (languages.isNotEmpty() || gradleFiles.isNotEmpty()) {
                AndroidProjectType.ANDROID_GRADLE
            } else AndroidProjectType.NONE
            return AndroidProjectAnalysis(
                type = type,
                packageName = packageName,
                versionName = versionName,
                versionCode = versionCode,
                minSdk = minSdk,
                targetSdk = targetSdk,
                compileSdk = compileSdk,
                agpVersion = agp,
                gradleVersion = gradleVersion,
                languages = languages,
                dependencies = dependencies,
                missingFiles = missing,
                issues = issues,
                warnings = warnings,
            )
        }

        val javaFiles = root.walkTopDown().any { it.isFile && it.extension.equals("java", true) }
        val kotlinFiles = root.walkTopDown().any { it.isFile && it.extension.equals("kt", true) }
        val web = File(root, "index.html").isFile || File(root, "package.json").isFile
        val type = when {
            kotlinFiles -> AndroidProjectType.KOTLIN
            javaFiles -> AndroidProjectType.JAVA
            web -> AndroidProjectType.WEB
            else -> AndroidProjectType.NONE
        }
        return AndroidProjectAnalysis(type = type, languages = languages)
    }
}
