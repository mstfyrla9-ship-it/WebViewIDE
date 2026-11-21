package dev.webviewide.project

import java.io.File

object ProjectBackup {
    fun create(project: WebProject, reason: String): File? {
        val backupRoot = File(project.dir, "backups")
        backupRoot.mkdirs()
        val stamp = System.currentTimeMillis()
        val safeReason = reason.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-').ifBlank { "backup" }
        val destination = File(backupRoot, "$stamp-$safeReason.zip")
        val source = project.dir
        val tmp = File(backupRoot, ".$destination.name.tmp")
        val ok = runCatching {
            ProjectZip.write(source, tmp)
            if (!tmp.renameTo(destination)) {
                tmp.copyTo(destination, overwrite = true)
                tmp.delete()
            }
            true
        }.getOrDefault(false)
        if (!ok) {
            tmp.delete()
            destination.delete()
            return null
        }
        return destination
    }

    fun list(project: WebProject): List<File> =
        File(project.dir, "backups")
            .listFiles { file -> file.isFile && file.extension.equals("zip", ignoreCase = true) }
            ?.sortedByDescending { it.lastModified() }
            .orEmpty()

    fun restore(project: WebProject, backup: File): Boolean {
        if (!backup.isFile || !SiteBackups.isInside(backup, project.dir)) return false
        val tmp = File(project.dir.parentFile, "${project.id}-restore-${System.currentTimeMillis()}")
        tmp.mkdirs()
        val restored = runCatching {
            backup.inputStream().use { ArchiveImport.unzip(it, tmp) }
            if (!File(tmp, "src").isDirectory && !tmp.walkTopDown().any { it.isFile }) {
                return@runCatching false
            }
            project.dir.listFiles()?.forEach { child ->
                if (child.name != "backups") child.deleteRecursively()
            }
            tmp.listFiles()?.forEach { child ->
                child.copyRecursively(File(project.dir, child.name), overwrite = true)
            }
            true
        }.getOrDefault(false)
        tmp.deleteRecursively()
        return restored
    }
}

private object SiteBackups {
    fun isInside(file: File, dir: File): Boolean =
        runCatching { file.canonicalFile.toPath().startsWith(dir.canonicalFile.toPath()) }.getOrDefault(false)
}
