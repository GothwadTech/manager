package com.gothwad.manager

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.webkit.MimeTypeMap
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.Collections
import java.util.Locale

object FileUtils {

    @JvmStatic
    fun uploadDirectory(context: Context): File {
        var dir = context.getExternalFilesDir("uploads")
        if (dir == null) {
            dir = File(context.filesDir, "uploads")
        }
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    @JvmStatic
    fun listFiles(directory: File?): List<File> {
        val result = mutableListOf<File>()
        val children = directory?.listFiles()
        if (children != null) {
            Collections.addAll(result, *children)
        }
        result.sortWith { left, right ->
            if (left.isDirectory != right.isDirectory) {
                if (left.isDirectory) -1 else 1
            } else {
                left.name.compareTo(right.name, ignoreCase = true)
            }
        }
        return result
    }

    @JvmStatic
    fun listUploads(context: Context): List<File> {
        return listUploadsForDirectory(uploadDirectory(context))
    }

    @JvmStatic
    fun listUploadsForDirectory(directory: File?): List<File> {
        val result = listFiles(directory).toMutableList()
        result.sortWith { left, right ->
            val delta = right.lastModified() - left.lastModified()
            if (delta == 0L) {
                left.name.compareTo(right.name, ignoreCase = true)
            } else if (delta < 0) -1 else 1
        }
        return result
    }

    @JvmStatic
    fun formatSize(bytes: Long): String {
        if (bytes < 1024) return "$bytes B"
        var value = bytes / 1024.0
        if (value < 1024) return String.format(Locale.US, "%.1f KB", value)
        value /= 1024.0
        if (value < 1024) return String.format(Locale.US, "%.1f MB", value)
        value /= 1024.0
        return String.format(Locale.US, "%.2f GB", value)
    }

    @JvmStatic
    fun mimeType(file: File): String {
        val name = file.name.lowercase(Locale.US)
        if (name.endsWith(".apk")) return "application/vnd.android.package-archive"
        val dot = name.lastIndexOf('.')
        if (dot >= 0 && dot + 1 < name.length) {
            val type = MimeTypeMap.getSingleton().getMimeTypeFromExtension(name.substring(dot + 1))
            if (type != null) return type
        }
        return "application/octet-stream"
    }

    @JvmStatic
    fun openFile(context: Context, file: File?) {
        if (file == null || !file.exists()) {
            Toast.makeText(context, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            return
        }
        if (file.isDirectory) return

        val apk = file.name.lowercase(Locale.US).endsWith(".apk")
        if (apk && Build.VERSION.SDK_INT >= 26) {
            val pm = context.packageManager
            if (!pm.canRequestPackageInstalls()) {
                try {
                    val permission = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:" + context.packageName)
                    )
                    context.startActivity(permission)
                    Toast.makeText(context, R.string.allow_unknown_sources_retry, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    Toast.makeText(context, R.string.allow_unknown_sources_settings, Toast.LENGTH_LONG).show()
                }
                return
            }
        }

        val uri = if (Build.VERSION.SDK_INT >= 24) {
            LocalFileProvider.uriForFile(context, file)
        } else {
            Uri.fromFile(file)
        }
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, mimeType(file))
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= 24) {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
        }
        try {
            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, R.string.no_app_to_open_file, Toast.LENGTH_LONG).show()
        }
    }

    @JvmStatic
    fun deleteRecursively(file: File?): Boolean {
        if (file == null || !file.exists()) return true
        if (file.isDirectory) {
            val children = file.listFiles()
            if (children != null) {
                for (child in children) {
                    if (!deleteRecursively(child)) return false
                }
            }
        }
        return file.delete()
    }

    @JvmStatic
    @Throws(IOException::class)
    fun safeDestination(
        directory: File,
        requestedName: String?,
        invalidNameMessage: String,
        duplicateNameMessage: String
    ): File {
        var clean = requestedName?.replace('\\', '/') ?: "upload.bin"
        clean = File(clean).name.trim()
        if (clean.isEmpty() || clean == "." || clean == "..") clean = "upload.bin"

        var candidate = File(directory, clean)
        val canonicalDir = directory.canonicalPath + File.separator
        if (!candidate.canonicalPath.startsWith(canonicalDir)) {
            throw IOException(invalidNameMessage)
        }
        if (!candidate.exists()) return candidate

        val dot = clean.lastIndexOf('.')
        val base = if (dot > 0) clean.substring(0, dot) else clean
        val extension = if (dot > 0) clean.substring(dot) else ""
        for (i in 1 until 10000) {
            candidate = File(directory, "$base ($i)$extension")
            if (!candidate.exists()) return candidate
        }
        throw IOException(duplicateNameMessage)
    }

    @JvmStatic
    fun sameFile(left: File?, right: File?): Boolean {
        if (left == null || right == null) return false
        return try {
            left.canonicalFile == right.canonicalFile
        } catch (e: IOException) {
            left.absoluteFile == right.absoluteFile
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun isSameOrDescendant(ancestor: File, candidate: File): Boolean {
        val ancestorPath = ancestor.canonicalPath
        val candidatePath = candidate.canonicalPath
        return candidatePath == ancestorPath || candidatePath.startsWith(ancestorPath + File.separator)
    }

    @JvmStatic
    fun pasteTargetForSelection(selected: File?): File? {
        if (selected == null) return null
        return if (selected.isDirectory) selected else selected.parentFile
    }

    @JvmStatic
    @Throws(IOException::class)
    fun paste(source: File, targetDirectory: File, move: Boolean): File {
        if (!source.exists()) throw IOException("Source does not exist")
        if (!targetDirectory.isDirectory) {
            throw IOException("Invalid target directory")
        }
        if (source.isDirectory && isSameOrDescendant(source, targetDirectory)) {
            throw IOException("Target is inside source")
        }

        val destination = uniqueDestination(targetDirectory, source.name, source.isFile)
        if (move && source.renameTo(destination)) return destination

        try {
            copyRecursively(source, destination)
        } catch (e: IOException) {
            deleteRecursively(destination)
            throw e
        }

        if (move && !deleteRecursively(source)) {
            throw IOException("Copied but could not remove source")
        }
        return destination
    }

    @Throws(IOException::class)
    private fun uniqueDestination(
        directory: File,
        originalName: String,
        preserveFileExtension: Boolean
    ): File {
        var candidate = File(directory, originalName)
        if (!candidate.exists()) return candidate

        val dot = if (preserveFileExtension) originalName.lastIndexOf('.') else -1
        val base = if (dot > 0) originalName.substring(0, dot) else originalName
        val extension = if (dot > 0) originalName.substring(dot) else ""
        for (i in 1 until 10000) {
            candidate = File(directory, "$base ($i)$extension")
            if (!candidate.exists()) return candidate
        }
        throw IOException("Too many duplicate names")
    }

    @Throws(IOException::class)
    private fun copyRecursively(source: File, destination: File) {
        if (source.isDirectory) {
            if (!destination.mkdir()) throw IOException("Could not create destination directory")
            val children = source.listFiles() ?: throw IOException("Could not read source directory")
            for (child in children) {
                copyRecursively(child, File(destination, child.name))
            }
            destination.setLastModified(source.lastModified())
        } else {
            copy(source, destination)
            destination.setLastModified(source.lastModified())
        }
    }

    @JvmStatic
    @Throws(IOException::class)
    fun copy(source: File, destination: File) {
        val input = FileInputStream(source)
        val output = FileOutputStream(destination)
        val buffer = ByteArray(64 * 1024)
        try {
            var count: Int
            while (input.read(buffer).also { count = it } != -1) {
                output.write(buffer, 0, count)
            }
            output.fd.sync()
        } finally {
            try { input.close() } catch (ignored: IOException) {}
            try { output.close() } catch (ignored: IOException) {}
        }
    }
}
