package dev.webviewide.build

import java.io.File
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertThrows
import org.junit.Test

class ApkSignatureVerifierTest {
    @Test
    fun rejectsApkWithoutValidSignature() {
        val dir = createTempDirectory("apk-verify").toFile()
        try {
            val unsigned = File(dir, "unsigned.apk")
            ZipOutputStream(unsigned.outputStream()).use { zip ->
                zip.putNextEntry(ZipEntry("AndroidManifest.xml"))
                zip.write("""<manifest xmlns:android="http://schemas.android.com/apk/res/android" package="dev.test"/>""".toByteArray())
                zip.closeEntry()
                zip.putNextEntry(ZipEntry("assets/test.txt"))
                zip.write("payload".toByteArray())
                zip.closeEntry()
            }

            assertThrows(IllegalStateException::class.java) {
                ApkSignatureVerifier.verifyOrThrow(unsigned)
            }
        } finally {
            dir.deleteRecursively()
        }
    }
}
