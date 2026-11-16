package com.hozinking.web2apk

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.util.UUID

enum class BlockType(val label: String, val icon: String) {
    JUDUL("Judul", "T"),
    TEKS("Teks", "≡"),
    GAMBAR("Gambar", "🖼"),
    TOMBOL("Tombol", "▣"),
    KARTU("Kartu", "▤"),
    PEMBATAS("Pembatas", "―"),
    SPASI("Spasi", "↕"),
    VIDEO("Video", "▶"),
    NAVBAR("Navbar", "☰"),
    FOOTER("Footer", "▂");

    companion object {
        fun defaultText(type: BlockType): String = when (type) {
            JUDUL -> "Judul Halaman"
            TEKS -> "Tulis teks di sini…"
            GAMBAR -> ""
            TOMBOL -> "Klik Saya"
            KARTU -> "Isi kartu"
            PEMBATAS -> ""
            SPASI -> ""
            VIDEO -> ""
            NAVBAR -> "Situs Saya"
            FOOTER -> "© 2026 Situs Saya"
        }

        fun defaultSizeSp(type: BlockType): Int = when (type) {
            JUDUL -> 28
            TEKS -> 16
            TOMBOL -> 16
            KARTU -> 16
            NAVBAR -> 20
            FOOTER -> 14
            else -> 16
        }
    }
}

data class Block(
    var id: String = UUID.randomUUID().toString(),
    var type: BlockType = BlockType.TEKS,
    var text: String = "",
    var textSizeSp: Int = 16,
    var colorHex: String = "#FFFFFF",
    var bgHex: String = "#1F2A33",
    var link: String = "",
    var imageName: String = "",
    var heightDp: Int = 96
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("type", type.name)
        put("text", text)
        put("textSizeSp", textSizeSp)
        put("colorHex", colorHex)
        put("bgHex", bgHex)
        put("link", link)
        put("imageName", imageName)
        put("heightDp", heightDp)
    }

    companion object {
        fun fromJson(o: JSONObject): Block = Block(
            id = o.optString("id", UUID.randomUUID().toString()),
            type = runCatching { BlockType.valueOf(o.optString("type", "TEKS")) }.getOrDefault(BlockType.TEKS),
            text = o.optString("text", ""),
            textSizeSp = o.optInt("textSizeSp", 16),
            colorHex = o.optString("colorHex", "#FFFFFF"),
            bgHex = o.optString("bgHex", "#1F2A33"),
            link = o.optString("link", ""),
            imageName = o.optString("imageName", ""),
            heightDp = o.optInt("heightDp", 96)
        )
    }
}

data class Project(
    var id: String = UUID.randomUUID().toString(),
    var name: String = "Proyek Baru",
    var updatedAt: Long = System.currentTimeMillis(),
    var blocks: MutableList<Block> = mutableListOf()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("name", name)
        put("updatedAt", updatedAt)
        val arr = JSONArray()
        blocks.forEach { arr.put(it.toJson()) }
        put("blocks", arr)
    }

    companion object {
        fun fromJson(o: JSONObject): Project {
            val blocks = mutableListOf<Block>()
            val arr = o.optJSONArray("blocks") ?: JSONArray()
            for (i in 0 until arr.length()) {
                runCatching { blocks.add(Block.fromJson(arr.getJSONObject(i))) }
            }
            return Project(
                id = o.optString("id", UUID.randomUUID().toString()),
                name = o.optString("name", "Proyek Baru"),
                updatedAt = o.optLong("updatedAt", System.currentTimeMillis()),
                blocks = blocks
            )
        }
    }
}

/** Penyimpanan proyek lokal sebagai JSON di filesDir/projects/<id>/project.json. */
object ProjectStore {
    private fun root(context: Context): File =
        File(context.filesDir, "projects").apply { mkdirs() }

    fun dirFor(context: Context, projectId: String): File =
        File(root(context), projectId).apply { mkdirs() }

    fun assetsDir(context: Context, projectId: String): File =
        File(dirFor(context, projectId), "assets").apply { mkdirs() }

    fun list(context: Context): List<Project> {
        val out = mutableListOf<Project>()
        val r = root(context)
        if (!r.exists()) return out
        r.listFiles()?.forEach { dir ->
            if (!dir.isDirectory) return@forEach
            val f = File(dir, "project.json")
            if (f.exists()) runCatching { out.add(Project.fromJson(JSONObject(f.readText()))) }
        }
        return out.sortedByDescending { it.updatedAt }
    }

    fun load(context: Context, id: String): Project? {
        val f = File(dirFor(context, id), "project.json")
        if (!f.exists()) return null
        return runCatching { Project.fromJson(JSONObject(f.readText())) }.getOrNull()
    }

    fun save(context: Context, project: Project) {
        project.updatedAt = System.currentTimeMillis()
        val f = File(dirFor(context, project.id), "project.json")
        f.writeText(project.toJson().toString())
    }

    fun delete(context: Context, id: String) {
        File(root(context), id).deleteRecursively()
    }

    fun duplicate(context: Context, project: Project): Project {
        val copy = Project(
            id = UUID.randomUUID().toString(),
            name = project.name + " (salinan)",
            blocks = project.blocks.map { it.copy(id = UUID.randomUUID().toString()) }.toMutableList()
        )
        // Salin aset gambar yang dipakai blok.
        val srcAssets = File(dirFor(context, project.id), "assets")
        if (srcAssets.exists()) {
            srcAssets.copyRecursively(assetsDir(context, copy.id), overwrite = true)
        }
        save(context, copy)
        return copy
    }

    fun slug(name: String): String {
        val s = name.lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
        return if (s.isEmpty()) "proyek" else s
    }
}
