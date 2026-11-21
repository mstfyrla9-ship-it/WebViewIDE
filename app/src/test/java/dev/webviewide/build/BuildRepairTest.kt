package dev.webviewide.build

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BuildRepairTest {
    @Test fun diagnosesSdkJdkAndDependencyProblems() {
        val log = """
            SDK location not found. Define location with an ANDROID_HOME environment variable.
            invalid source release: 21
            Could not resolve all dependencies for configuration.
        """.trimIndent()
        val suggestions = BuildRepair.diagnose(log)
        assertTrue(suggestions.any { it.title.contains("SDK") })
        assertTrue(suggestions.any { it.title.contains("JDK") })
        assertTrue(suggestions.any { it.title.contains("dependency", ignoreCase = true) })
    }

    @Test fun aiPromptForbidsSecretOutputAndNamesBackupNeed() {
        val prompt = BuildRepair.aiPrompt("Manifest merge failed", "Android Gradle project")
        assertTrue(prompt.contains("Do not include passwords"))
        assertTrue(prompt.contains("backup"))
        assertTrue(prompt.contains("Manifest merge failed"))
    }

    @Test fun supportReportRedactsPasswordsAndTokens() {
        val report = SupportReport.create(
            "Signing failure",
            "Release signing failed",
            "storePassword=topsecret\nAuthorization: Bearer abcdef",
            "com.example.app",
        )
        assertFalse(report.body.contains("topsecret"))
        assertFalse(report.body.contains("abcdef"))
        assertTrue(report.body.contains("[REDACTED]"))
        assertTrue(report.body.contains("Android"))
    }
}
