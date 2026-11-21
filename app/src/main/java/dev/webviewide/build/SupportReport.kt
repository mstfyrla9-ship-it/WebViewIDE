package dev.webviewide.build

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import dev.webviewide.BuildConfig

object SupportReport {
    const val EMAIL = "mstfyrla9@gmail.com"

    data class Report(
        val subject: String,
        val body: String,
    )

    fun create(type: String, description: String, buildLog: String, projectSummary: String): Report {
        val safeDescription = description.trim().ifBlank { "(No description provided)" }
        val safeLog = buildLog
            .replace(Regex("""(?i)(password|keystore|key_password|store_password)\s*[=:]\s*\S+"""), "$1=[REDACTED]")
            .replace(Regex("""(?i)(bearer\s+)[A-Za-z0-9._~-]+"""), "$1[REDACTED]")
            .take(12_000)
        val body = buildString {
            appendLine("WebViewIDE support request")
            appendLine("Type: $type")
            appendLine()
            appendLine("Description:")
            appendLine(safeDescription)
            appendLine()
            appendLine("App: ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})")
            appendLine("Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
            appendLine("Device: ${Build.MANUFACTURER} ${Build.MODEL}")
            appendLine()
            appendLine("Project summary:")
            appendLine(projectSummary.ifBlank { "(not available)" })
            appendLine()
            appendLine("Build log (secrets redacted):")
            appendLine(safeLog.ifBlank { "(not available)" })
        }
        return Report("[WebViewIDE] $type", body)
    }

    fun reviewAndSend(context: Context, report: Report) {
        val intent = Intent(Intent.ACTION_SENDTO).apply {
            data = Uri.parse("mailto:")
            putExtra(Intent.EXTRA_EMAIL, arrayOf(EMAIL))
            putExtra(Intent.EXTRA_SUBJECT, report.subject)
            putExtra(Intent.EXTRA_TEXT, report.body)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(intent, "Send support report").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
}
