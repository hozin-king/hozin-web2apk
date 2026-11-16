package com.hozinking.web2apk

/** Generate index.html bersih + responsif dari blok proyek. */
object HtmlGenerator {

    fun escape(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")

    fun youtubeId(url: String): String? {
        val u = url.trim()
        if (u.isEmpty()) return null
        // youtu.be/<id>
        Regex("youtu\\.be/([A-Za-z0-9_-]{6,})").find(u)?.let { return it.groupValues[1] }
        // youtube.com/watch?v=<id> atau /embed/<id> atau /shorts/<id>
        Regex("[?&]v=([A-Za-z0-9_-]{6,})").find(u)?.let { return it.groupValues[1] }
        Regex("(?:embed|shorts|live)/([A-Za-z0-9_-]{6,})").find(u)?.let { return it.groupValues[1] }
        return null
    }

    fun generate(project: Project): String {
        val sb = StringBuilder()
        sb.append("<!DOCTYPE html>\n<html lang=\"id\">\n<head>\n<meta charset=\"utf-8\">\n")
        sb.append("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n")
        sb.append("<title>").append(escape(project.name)).append("</title>\n")
        sb.append("<style>\n")
        sb.append("body{margin:0;font-family:system-ui,-apple-system,'Segoe UI',sans-serif;background:#12181d;color:#e6edef;}\n")
        sb.append(".wrap{max-width:720px;margin:0 auto;padding:16px;}\n")
        sb.append("h1{margin:16px 0;}\n")
        sb.append("p{line-height:1.6;}\n")
        sb.append("img{max-width:100%;height:auto;display:block;border-radius:12px;}\n")
        sb.append(".btn{display:inline-block;padding:12px 24px;border-radius:999px;text-decoration:none;font-weight:700;}\n")
        sb.append(".card{border-radius:16px;padding:20px;margin:16px 0;}\n")
        sb.append("hr{border:none;border-top:1px solid #33414d;margin:24px 0;}\n")
        sb.append(".nav{display:flex;align-items:center;gap:16px;padding:14px 16px;background:#1b2530;}\n")
        sb.append(".nav .brand{font-weight:800;font-size:20px;}\n")
        sb.append(".nav a{color:#2dd4bf;text-decoration:none;}\n")
        sb.append("footer{text-align:center;padding:24px 16px;color:#8a9ba8;font-size:14px;}\n")
        sb.append(".video{position:relative;width:100%;aspect-ratio:16/9;background:#000;border-radius:12px;overflow:hidden;}\n")
        sb.append(".video iframe,.video video{position:absolute;inset:0;width:100%;height:100%;border:0;}\n")
        sb.append("</style>\n</head>\n<body>\n")

        var inWrap = false
        fun openWrap() {
            if (!inWrap) {
                sb.append("<div class=\"wrap\">\n")
                inWrap = true
            }
        }
        fun closeWrap() {
            if (inWrap) {
                sb.append("</div>\n")
                inWrap = false
            }
        }

        for (block in project.blocks) {
            when (block.type) {
                BlockType.NAVBAR -> {
                    closeWrap()
                    sb.append("<nav class=\"nav\"><span class=\"brand\" style=\"color:")
                        .append(escape(block.colorHex)).append(";font-size:")
                        .append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text.ifEmpty { "Situs Saya" })).append("</span>")
                    if (block.link.isNotBlank()) {
                        sb.append("<a href=\"").append(escape(block.link)).append("\">")
                            .append(escape(block.link)).append("</a>")
                    }
                    sb.append("</nav>\n")
                }
                BlockType.FOOTER -> {
                    closeWrap()
                    sb.append("<footer style=\"color:").append(escape(block.colorHex))
                        .append(";font-size:").append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text)).append("</footer>\n")
                }
                BlockType.JUDUL -> {
                    openWrap()
                    sb.append("<h1 style=\"color:").append(escape(block.colorHex))
                        .append(";font-size:").append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text)).append("</h1>\n")
                }
                BlockType.TEKS -> {
                    openWrap()
                    sb.append("<p style=\"color:").append(escape(block.colorHex))
                        .append(";font-size:").append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text)).append("</p>\n")
                }
                BlockType.GAMBAR -> {
                    openWrap()
                    if (block.imageName.isNotBlank()) {
                        sb.append("<img src=\"assets/").append(escape(block.imageName))
                            .append("\" alt=\"").append(escape(block.text)).append("\">\n")
                    }
                }
                BlockType.TOMBOL -> {
                    openWrap()
                    sb.append("<p><a class=\"btn\" href=\"").append(escape(block.link.ifBlank { "#" }))
                        .append("\" style=\"background:").append(escape(block.bgHex))
                        .append(";color:").append(escape(block.colorHex))
                        .append(";font-size:").append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text)).append("</a></p>\n")
                }
                BlockType.KARTU -> {
                    openWrap()
                    sb.append("<div class=\"card\" style=\"background:").append(escape(block.bgHex))
                        .append(";color:").append(escape(block.colorHex))
                        .append(";font-size:").append(block.textSizeSp).append("px;\">")
                        .append(escape(block.text)).append("</div>\n")
                }
                BlockType.PEMBATAS -> {
                    openWrap()
                    sb.append("<hr style=\"border-top-color:").append(escape(block.bgHex)).append("\">\n")
                }
                BlockType.SPASI -> {
                    openWrap()
                    sb.append("<div style=\"height:").append(block.heightDp).append("px;\"></div>\n")
                }
                BlockType.VIDEO -> {
                    openWrap()
                    val yt = youtubeId(block.link)
                    sb.append("<div class=\"video\">")
                    if (yt != null) {
                        sb.append("<iframe src=\"https://www.youtube.com/embed/")
                            .append(yt)
                            .append("\" allowfullscreen></iframe>")
                    } else if (block.link.isNotBlank()) {
                        sb.append("<video controls src=\"").append(escape(block.link)).append("\"></video>")
                    }
                    sb.append("</div>\n")
                }
            }
        }
        closeWrap()
        sb.append("</body>\n</html>\n")
        return sb.toString()
    }
}
