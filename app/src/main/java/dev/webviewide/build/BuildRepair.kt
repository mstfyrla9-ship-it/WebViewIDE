package dev.webviewide.build

data class RepairSuggestion(
    val title: String,
    val detail: String,
    val confidence: String,
)

object BuildRepair {
    fun diagnose(log: String): List<RepairSuggestion> {
        val text = log.lowercase()
        val suggestions = mutableListOf<RepairSuggestion>()
        if ("sdk location" in text || "android_home" in text || "android sdk" in text) {
            suggestions += RepairSuggestion(
                "Configure Android SDK",
                "Set sdk.dir in local.properties or ANDROID_HOME, and verify platforms/android-<targetSdk>/android.jar exists.",
                "high",
            )
        }
        if ("jdk" in text || "java_home" in text || "invalid source release" in text) {
            suggestions += RepairSuggestion(
                "Check JDK compatibility",
                "Use JDK 17 for current Android Gradle Plugin builds and set JAVA_HOME explicitly.",
                "high",
            )
        }
        if ("gradle" in text && ("distribution" in text || "wrapper" in text || "could not download" in text)) {
            suggestions += RepairSuggestion(
                "Repair Gradle Wrapper",
                "Verify gradle/wrapper/gradle-wrapper.properties and its distributionUrl. Re-download only the pinned distribution.",
                "high",
            )
        }
        if ("could not resolve" in text || "dependency" in text && "failed" in text) {
            suggestions += RepairSuggestion(
                "Resolve dependency conflict",
                "Run dependency insight, pin conflicting versions in version catalogs, and retry only after network/cache access is available.",
                "medium",
            )
        }
        if ("manifest" in text && ("attribute" in text || "merge" in text || "package" in text)) {
            suggestions += RepairSuggestion(
                "Resolve Manifest conflict",
                "Inspect main and library manifests for duplicate attributes or package/applicationId mismatches. Fix the declaring module, not generated output.",
                "medium",
            )
        }
        if ("resource" in text || "aapt" in text || "xml" in text) {
            suggestions += RepairSuggestion(
                "Repair resources/XML",
                "Check malformed XML, missing drawable/layout resources, and resource names that differ by case or locale.",
                "medium",
            )
        }
        if ("kotlin" in text && ("version" in text || "compat" in text)) {
            suggestions += RepairSuggestion(
                "Align Kotlin and AGP versions",
                "Use a Kotlin/AGP combination supported by the project, and update plugins and compiler extensions together.",
                "medium",
            )
        }
        if (suggestions.isEmpty() && log.isNotBlank()) {
            suggestions += RepairSuggestion(
                "Review the first build error",
                "Start from the earliest error before cascading failures. Preserve source files and create a backup before editing.",
                "low",
            )
        }
        return suggestions
    }

    fun aiPrompt(log: String, projectSummary: String): String = """
        Analyze this Android build failure.
        Project summary:
        $projectSummary

        Return a short diagnosis, exact affected file(s), a minimal safe patch, and risks.
        Do not invent paths or changes. Do not include passwords or secrets.
        If a change may overwrite source, state that it requires a backup and explicit confirmation.

        Build log:
        ${log.take(20_000)}
    """.trimIndent()
}
