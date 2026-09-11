package com.gothwad.manager

import android.content.Context
import android.content.SharedPreferences
import android.os.Environment
import org.json.JSONArray
import java.io.File

object BookmarkManager {

    private const val PREF_BOOKMARKS = "gothwad_bookmarks"
    private const val KEY_LIST = "bookmarked_paths"

    fun getBookmarks(context: Context): List<File> {
        val prefs = context.getSharedPreferences(PREF_BOOKMARKS, Context.MODE_PRIVATE)
        val raw = prefs.getString(KEY_LIST, null)
        val list = mutableListOf<File>()

        if (raw.isNullOrEmpty()) {
            // Default common TV media folders
            val ext = Environment.getExternalStorageDirectory()
            if (ext != null) {
                listOf("Download", "Movies", "DCIM", "Music", "Documents").forEach { folder ->
                    val f = File(ext, folder)
                    if (f.exists() && f.isDirectory) {
                        list.add(f)
                    }
                }
            }
            saveBookmarks(context, list)
            return list
        }

        try {
            val jsonArray = JSONArray(raw)
            for (i in 0 until jsonArray.length()) {
                val path = jsonArray.getString(i)
                val file = File(path)
                if (file.exists() && file.isDirectory) {
                    list.add(file)
                }
            }
        } catch (ignored: Exception) {}
        return list
    }

    fun isBookmarked(context: Context, directory: File): Boolean {
        return getBookmarks(context).any { it.absolutePath == directory.absolutePath }
    }

    fun addBookmark(context: Context, directory: File): Boolean {
        if (!directory.isDirectory) return false
        val current = getBookmarks(context).toMutableList()
        if (current.none { it.absolutePath == directory.absolutePath }) {
            current.add(directory)
            saveBookmarks(context, current)
            return true
        }
        return false
    }

    fun removeBookmark(context: Context, directory: File): Boolean {
        val current = getBookmarks(context).toMutableList()
        val removed = current.removeAll { it.absolutePath == directory.absolutePath }
        if (removed) {
            saveBookmarks(context, current)
        }
        return removed
    }

    private fun saveBookmarks(context: Context, list: List<File>) {
        val prefs = context.getSharedPreferences(PREF_BOOKMARKS, Context.MODE_PRIVATE)
        val array = JSONArray()
        for (f in list) {
            array.put(f.absolutePath)
        }
        prefs.edit().putString(KEY_LIST, array.toString()).apply()
    }
}
