package dev.webviewide.build

import java.util.concurrent.atomic.AtomicBoolean

enum class BuildVariant { DEBUG, RELEASE }

data class BuildReport(
    val success: Boolean,
    val variant: BuildVariant,
    val stages: List<BuildStage>,
    val log: List<String>,
    val artifact: BuildArtifact? = null,
    val error: String? = null,
)

object AndroidBuildPipeline {
    private val stageNames = listOf(
        "Analyze project",
        "Check tools",
        "Check dependencies",
        "Resolve build problems",
        "Build",
        "Analyze errors",
        "Auto repair",
        "Rebuild",
        "Zipalign",
        "Sign",
        "Verify signature",
        "Validate APK",
        "Save output",
        "Create report",
    )

    fun stages(): List<BuildStage> = stageNames.map { BuildStage(it) }

    fun cancelled(cancelRequested: AtomicBoolean): Boolean = cancelRequested.get()

    fun stage(
        stages: MutableList<BuildStage>,
        index: Int,
        status: StageStatus,
        detail: String = "",
    ) {
        if (index in stages.indices) {
            stages[index] = stages[index].copy(status = status, detail = detail)
        }
    }

    fun report(
        variant: BuildVariant,
        stages: List<BuildStage>,
        log: List<String>,
        artifact: BuildArtifact?,
        error: String?,
    ): BuildReport = BuildReport(
        success = error == null && artifact != null,
        variant = variant,
        stages = stages,
        log = log,
        artifact = artifact,
        error = error,
    )
}
