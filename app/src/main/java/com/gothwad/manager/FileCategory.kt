package com.gothwad.manager

import java.io.File
import java.util.Locale

enum class FileCategory(val labelRes: Int, val badgeText: String) {
    ALL(R.string.category_all, "ALL"),
    VIDEOS(R.string.category_videos, "VIDEO"),
    MUSIC(R.string.category_music, "AUDIO"),
    PHOTOS(R.string.category_photos, "IMAGE"),
    APKS(R.string.category_apks, "APK"),
    DOCUMENTS(R.string.category_documents, "DOC"),
    ARCHIVES(R.string.category_archives, "ZIP");

    fun matches(file: File): Boolean {
        if (file.isDirectory) return false
        val ext = file.extension.lowercase(Locale.US)
        return when (this) {
            ALL -> true
            VIDEOS -> ext in VIDEO_EXTENSIONS
            MUSIC -> ext in AUDIO_EXTENSIONS
            PHOTOS -> ext in IMAGE_EXTENSIONS
            APKS -> ext == "apk" || ext == "xapk"
            DOCUMENTS -> ext in DOC_EXTENSIONS
            ARCHIVES -> ext in ARCHIVE_EXTENSIONS
        }
    }

    companion object {
        private val VIDEO_EXTENSIONS = setOf("mp4", "mkv", "avi", "mov", "webm", "ts", "m4v", "3gp", "flv", "wmv")
        private val AUDIO_EXTENSIONS = setOf("mp3", "m4a", "flac", "wav", "ogg", "aac", "wma", "opus")
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "webp", "gif", "bmp", "svg")
        private val DOC_EXTENSIONS = setOf("pdf", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "txt", "epub", "json", "xml", "log", "md", "csv", "m3u")
        private val ARCHIVE_EXTENSIONS = setOf("zip", "rar", "7z", "tar", "gz", "bz2", "xz")

        fun filterDirectory(directory: File?, category: FileCategory): List<File> {
            if (directory == null || !directory.exists() || !directory.isDirectory) return emptyList()
            val files = directory.listFiles() ?: return emptyList()
            if (category == ALL) return files.toList()
            return files.filter { category.matches(it) }
        }

        fun scanCategory(root: File?, category: FileCategory, maxResults: Int = 300): List<File> {
            if (root == null || !root.exists() || !root.isDirectory) return emptyList()
            val results = mutableListOf<File>()
            val stack = ArrayDeque<File>()
            stack.add(root)

            while (stack.isNotEmpty() && results.size < maxResults) {
                val current = stack.removeFirst()
                val children = current.listFiles() ?: continue
                for (child in children) {
                    if (child.isDirectory) {
                        // Skip hidden system or android cache folders for speed
                        if (!child.name.startsWith(".") && child.name != "Android") {
                            stack.add(child)
                        }
                    } else if (category.matches(child)) {
                        results.add(child)
                        if (results.size >= maxResults) break
                    }
                }
            }
            return results
        }
    }
}
