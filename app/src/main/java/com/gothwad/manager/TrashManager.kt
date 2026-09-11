package com.gothwad.manager

import android.content.Context
import android.os.Environment
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.IOException

data class TrashItem(
    val id: String,
    val file: File,
    val originalPath: String,
    val trashedAt: Long
)

object TrashManager {

    private const val TRASH_DIR_NAME = ".gothwad_trash"
    private const val META_FILE_NAME = "trash_metadata.json"

    private fun getTrashDir(context: Context): File {
        val root = Environment.getExternalStorageDirectory() ?: context.filesDir
        val dir = File(root, TRASH_DIR_NAME)
        if (!dir.exists()) dir.mkdirs()
        return dir
    }

    private fun getMetaFile(context: Context): File {
        return File(getTrashDir(context), META_FILE_NAME)
    }

    @Synchronized
    private fun loadMetadata(context: Context): MutableList<TrashItem> {
        val list = mutableListOf<TrashItem>()
        val file = getMetaFile(context)
        if (!file.exists()) return list
        try {
            val jsonStr = file.readText()
            val array = JSONArray(jsonStr)
            val trashDir = getTrashDir(context)
            for (i in 0 until array.length()) {
                val obj = array.getJSONObject(i)
                val id = obj.optString("id")
                val diskName = obj.optString("diskName")
                val orig = obj.optString("originalPath")
                val time = obj.optLong("trashedAt")
                val diskFile = File(trashDir, diskName)
                if (diskFile.exists()) {
                    list.add(TrashItem(id, diskFile, orig, time))
                }
            }
        } catch (ignored: Exception) {}
        return list
    }

    @Synchronized
    private fun saveMetadata(context: Context, items: List<TrashItem>) {
        try {
            val array = JSONArray()
            for (item in items) {
                val obj = JSONObject()
                obj.put("id", item.id)
                obj.put("diskName", item.file.name)
                obj.put("originalPath", item.originalPath)
                obj.put("trashedAt", item.trashedAt)
                array.put(obj)
            }
            getMetaFile(context).writeText(array.toString())
        } catch (ignored: Exception) {}
    }

    @Synchronized
    fun moveToTrash(context: Context, file: File): Boolean {
        if (!file.exists()) return false
        val trashDir = getTrashDir(context)
        val id = System.currentTimeMillis().toString() + "_" + (100..999).random()
        val diskName = "${id}_${file.name}"
        val dest = File(trashDir, diskName)

        val success = if (file.renameTo(dest)) {
            true
        } else {
            try {
                FileUtils.copy(file, dest)
                FileUtils.deleteRecursively(file)
                true
            } catch (e: Exception) {
                false
            }
        }

        if (success) {
            val items = loadMetadata(context)
            items.add(TrashItem(id, dest, file.absolutePath, System.currentTimeMillis()))
            saveMetadata(context, items)
            return true
        }
        return false
    }

    @Synchronized
    fun getTrashItems(context: Context): List<TrashItem> {
        return loadMetadata(context).sortedByDescending { it.trashedAt }
    }

    @Synchronized
    fun getTrashCount(context: Context): Int {
        return loadMetadata(context).size
    }

    @Synchronized
    fun restoreItem(context: Context, item: TrashItem): Boolean {
        val dest = File(item.originalPath)
        val parent = dest.parentFile
        if (parent != null && !parent.exists()) {
            parent.mkdirs()
        }

        val target = if (dest.exists()) {
            val name = dest.nameWithoutExtension
            val ext = if (dest.extension.isNotEmpty()) ".${dest.extension}" else ""
            File(parent ?: getTrashDir(context), "${name}_restored$ext")
        } else {
            dest
        }

        val success = if (item.file.renameTo(target)) {
            true
        } else {
            try {
                FileUtils.copy(item.file, target)
                FileUtils.deleteRecursively(item.file)
                true
            } catch (e: Exception) {
                false
            }
        }

        if (success) {
            val items = loadMetadata(context).filter { it.id != item.id }
            saveMetadata(context, items)
            return true
        }
        return false
    }

    @Synchronized
    fun deletePermanently(context: Context, item: TrashItem): Boolean {
        val deleted = FileUtils.deleteRecursively(item.file)
        val items = loadMetadata(context).filter { it.id != item.id }
        saveMetadata(context, items)
        return deleted
    }

    @Synchronized
    fun emptyTrash(context: Context): Boolean {
        val trashDir = getTrashDir(context)
        val metaFile = getMetaFile(context)
        val children = trashDir.listFiles() ?: return true
        for (child in children) {
            FileUtils.deleteRecursively(child)
        }
        metaFile.delete()
        return true
    }
}
