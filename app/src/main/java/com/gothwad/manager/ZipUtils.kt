package com.gothwad.manager

import java.io.BufferedInputStream
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

object ZipUtils {

    @Throws(IOException::class)
    fun extractZip(zipFile: File, destinationDir: File): Int {
        if (!destinationDir.exists()) {
            destinationDir.mkdirs()
        }
        val destCanonical = destinationDir.canonicalPath
        var extractedCount = 0

        ZipInputStream(BufferedInputStream(FileInputStream(zipFile))).use { zis ->
            var entry: ZipEntry? = zis.nextEntry
            val buffer = ByteArray(8192)

            while (entry != null) {
                val newFile = File(destinationDir, entry.name)
                val newFileCanonical = newFile.canonicalPath

                // Zip Slip protection
                if (!newFileCanonical.startsWith(destCanonical)) {
                    throw IOException("Zip entry attempted directory traversal: ${entry.name}")
                }

                if (entry.isDirectory) {
                    newFile.mkdirs()
                } else {
                    val parent = newFile.parentFile
                    if (parent != null && !parent.exists()) {
                        parent.mkdirs()
                    }

                    FileOutputStream(newFile).use { fos ->
                        BufferedOutputStream(fos).use { bos ->
                            var len: Int
                            while (zis.read(buffer).also { len = it } != -1) {
                                bos.write(buffer, 0, len)
                            }
                        }
                    }
                    extractedCount++
                }
                zis.closeEntry()
                entry = zis.nextEntry
            }
        }
        return extractedCount
    }

    @Throws(IOException::class)
    fun compressFilesToZip(files: List<File>, destinationZip: File): Long {
        if (files.isEmpty()) return 0L
        val parent = destinationZip.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        var totalEntries = 0L
        ZipOutputStream(BufferedOutputStream(FileOutputStream(destinationZip))).use { zos ->
            val buffer = ByteArray(8192)
            for (file in files) {
                totalEntries += addFileToZip("", file, zos, buffer)
            }
        }
        return totalEntries
    }

    @Throws(IOException::class)
    private fun addFileToZip(basePath: String, file: File, zos: ZipOutputStream, buffer: ByteArray): Long {
        var count = 0L
        val entryName = if (basePath.isEmpty()) file.name else "$basePath/${file.name}"

        if (file.isDirectory) {
            val dirEntryName = if (entryName.endsWith("/")) entryName else "$entryName/"
            zos.putNextEntry(ZipEntry(dirEntryName))
            zos.closeEntry()
            count++

            val children = file.listFiles() ?: return count
            for (child in children) {
                count += addFileToZip(entryName, child, zos, buffer)
            }
        } else {
            zos.putNextEntry(ZipEntry(entryName))
            FileInputStream(file).use { fis ->
                BufferedInputStream(fis).use { bis ->
                    var len: Int
                    while (bis.read(buffer).also { len = it } != -1) {
                        zos.write(buffer, 0, len)
                    }
                }
            }
            zos.closeEntry()
            count++
        }
        return count
    }
}
