package dev.webviewide.build

import com.android.apksig.ApkVerifier
import com.android.apksig.apk.ApkFormatException
import java.io.File

internal object ApkSignatureVerifier {
    fun verifyOrThrow(apk: File) {
        require(apk.isFile && apk.length() > 0L) { "APK file is missing or empty" }
        val result = try {
            ApkVerifier.Builder(apk).build().verify()
        } catch (e: ApkFormatException) {
            throw IllegalStateException("APK signature verification failed: ${e.message}", e)
        }
        if (!result.isVerified) {
            val details = result.errors.joinToString("; ") { it.toString() }
                .ifBlank { "no signature scheme verified the APK" }
            throw IllegalStateException("APK signature verification failed: $details")
        }
    }
}
