package com.gothwad.manager

import android.os.Environment
import java.io.File
import java.util.ArrayDeque

object RecentFilesManager {

    fun scanRecentFiles(root: File? = null, maxAgeHours: Int = 48, maxCount: Int = 100): List<File> {
        val base = root ?: Environment.getExternalStorageDirectory() ?: return emptyList()
        val now = System.currentTimeMillis()
        val threshold = now - (maxAgeHours.toLong() * 3600 * 1000)

        val results = mutableListOf<File>()
        val queue = ArrayDeque<File>()
        queue.add(base)

        while (queue.isNotEmpty() && results.size < 300) {
            val dir = queue.removeFirst()
            val children = dir.listFiles() ?: continue
            for (child in children) {
                val name = child.name
                if (name.startsWith(".") || name == "Android") continue
                if (child.isDirectory) {
                    queue.add(child)
                } else {
                    if (child.lastModified() >= threshold) {
                        results.add(child)
                    }
                }
            }
        }

        return results.sortedByDescending { it.lastModified() }.take(maxCount)
    }
}
