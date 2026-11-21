package dev.webviewide.project

import java.io.File
import kotlin.io.path.createTempDirectory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AndroidProjectAnalysisTest {
    @Test fun detectsGradleAndroidProject() {
        val root = createTempDirectory("android-project").toFile()
        try {
            File(root, "gradle/wrapper").mkdirs()
            File(root, "gradle/wrapper/gradle-wrapper.properties").writeText(
                "distributionUrl=https\\://services.gradle.org/distributions/gradle-8.10.2-bin.zip",
            )
            File(root, "app/src/main").mkdirs()
            File(root, "app/src/main/AndroidManifest.xml").writeText(
                """<manifest package="com.example.demo" android:versionName="2.1" android:versionCode="21" xmlns:android="http://schemas.android.com/apk/res/android"/>""",
            )
            File(root, "app/build.gradle.kts").writeText(
                """
                plugins { id("com.android.application") }
                android {
                    namespace = "com.example.demo"
                    compileSdk = 35
                    defaultConfig {
                        applicationId = "com.example.demo"
                        minSdk = 24
                        targetSdk = 35
                        versionName = "2.1"
                        versionCode = 21
                    }
                }
                """.trimIndent(),
            )

            val result = AndroidProjectAnalysisEngine.analyze(root)
            assertEquals(AndroidProjectType.ANDROID_GRADLE, result.type)
            assertEquals("com.example.demo", result.packageName)
            assertEquals("2.1", result.versionName)
            assertEquals(21, result.versionCode)
            assertEquals(24, result.minSdk)
            assertEquals(35, result.targetSdk)
            assertEquals(35, result.compileSdk)
            assertEquals("8.10.2", result.gradleVersion)
            assertTrue(result.languages.contains("XML"))
        } finally {
            root.deleteRecursively()
        }
    }

    @Test fun marksMissingWrapperAndManifest() {
        val root = createTempDirectory("android-incomplete").toFile()
        try {
            File(root, "build.gradle").writeText("""plugins { id("com.android.application") }""")
            val result = AndroidProjectAnalysisEngine.analyze(root)
            assertEquals(AndroidProjectType.ANDROID_GRADLE, result.type)
            assertTrue(result.missingFiles.contains("AndroidManifest.xml"))
            assertTrue(result.missingFiles.contains("gradle-wrapper.properties"))
            assertTrue(result.issues.isNotEmpty())
        } finally {
            root.deleteRecursively()
        }
    }
}
