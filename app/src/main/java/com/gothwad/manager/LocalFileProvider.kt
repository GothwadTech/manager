package com.gothwad.manager

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.MatrixCursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import java.io.File
import java.io.FileNotFoundException

class LocalFileProvider : ContentProvider() {

    companion object {
        private const val PARAM_PATH = "path"

        @JvmStatic
        fun uriForFile(context: Context, file: File): Uri {
            return Uri.Builder()
                .scheme("content")
                .authority(context.packageName + ".files")
                .appendPath("file")
                .appendQueryParameter(PARAM_PATH, file.absolutePath)
                .build()
        }
    }

    override fun onCreate(): Boolean = true

    @Throws(FileNotFoundException::class)
    private fun fileFromUri(uri: Uri): File {
        val path = uri.getQueryParameter(PARAM_PATH) ?: throw FileNotFoundException("Missing path")
        val file = File(path)
        if (!file.exists() || !file.isFile || !file.canRead()) {
            throw FileNotFoundException(path)
        }
        return file
    }

    override fun getType(uri: Uri): String {
        return try {
            FileUtils.mimeType(fileFromUri(uri))
        } catch (e: FileNotFoundException) {
            "application/octet-stream"
        }
    }

    @Throws(FileNotFoundException::class)
    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor {
        if ("r" != mode) throw FileNotFoundException("Read only")
        return ParcelFileDescriptor.open(fileFromUri(uri), ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? {
        return try {
            val file = fileFromUri(uri)
            val cursor = MatrixCursor(arrayOf(OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE))
            cursor.addRow(arrayOf<Any>(file.name, file.length()))
            cursor
        } catch (e: FileNotFoundException) {
            null
        }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri {
        throw UnsupportedOperationException()
    }

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
