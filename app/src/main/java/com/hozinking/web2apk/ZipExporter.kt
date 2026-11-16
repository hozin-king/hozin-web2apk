package com.hozinking.web2apk

import android.content.Context
import java.io.File
import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/** Export proyek sebagai ZIP: index.html + assets/ (gambar sebagai file). */
object ZipExporter {
    fun exportZip(context: Context, project: Project): File {
        val outDir = File(context.cacheDir, "export").apply { mkdirs() }
        val zipFile = File(outDir, ProjectStore.slug(project.name) + ".zip")
        val html = HtmlGenerator.generate(project)
        ZipOutputStream(FileOutputStream(zipFile)).use { zos ->
            zos.putNextEntry(ZipEntry("index.html"))
            zos.write(html.toByteArray(Charsets.UTF_8))
            zos.closeEntry()
            val assets = ProjectStore.assetsDir(context, project.id)
            assets.listFiles()?.forEach { f ->
                if (!f.isFile) return@forEach
                zos.putNextEntry(ZipEntry("assets/" + f.name))
                f.inputStream().use { it.copyTo(zos) }
                zos.closeEntry()
            }
        }
        return zipFile
    }
}
