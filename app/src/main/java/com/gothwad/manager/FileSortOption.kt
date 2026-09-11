package com.gothwad.manager

import java.io.File
import java.util.Locale

enum class FileSortOption(val titleRes: Int) {
    NAME_ASC(R.string.sort_name_asc),
    NAME_DESC(R.string.sort_name_desc),
    DATE_DESC(R.string.sort_date_desc),
    DATE_ASC(R.string.sort_date_asc),
    SIZE_DESC(R.string.sort_size_desc),
    SIZE_ASC(R.string.sort_size_asc),
    TYPE_ASC(R.string.sort_type);

    companion object {
        fun sort(files: List<File>, option: FileSortOption): List<File> {
            val list = files.toMutableList()
            list.sortWith { a, b ->
                // Always group directories on top for standard file manager experience
                if (a.isDirectory != b.isDirectory) {
                    return@sortWith if (a.isDirectory) -1 else 1
                }

                when (option) {
                    NAME_ASC -> a.name.compareTo(b.name, ignoreCase = true)
                    NAME_DESC -> b.name.compareTo(a.name, ignoreCase = true)
                    DATE_DESC -> {
                        val delta = b.lastModified() - a.lastModified()
                        if (delta == 0L) a.name.compareTo(b.name, ignoreCase = true)
                        else if (delta < 0) -1 else 1
                    }
                    DATE_ASC -> {
                        val delta = a.lastModified() - b.lastModified()
                        if (delta == 0L) a.name.compareTo(b.name, ignoreCase = true)
                        else if (delta < 0) -1 else 1
                    }
                    SIZE_DESC -> {
                        val delta = b.length() - a.length()
                        if (delta == 0L) a.name.compareTo(b.name, ignoreCase = true)
                        else if (delta < 0) -1 else 1
                    }
                    SIZE_ASC -> {
                        val delta = a.length() - b.length()
                        if (delta == 0L) a.name.compareTo(b.name, ignoreCase = true)
                        else if (delta < 0) -1 else 1
                    }
                    TYPE_ASC -> {
                        val extA = a.extension.lowercase(Locale.US)
                        val extB = b.extension.lowercase(Locale.US)
                        val comp = extA.compareTo(extB)
                        if (comp != 0) comp else a.name.compareTo(b.name, ignoreCase = true)
                    }
                }
            }
            return list
        }
    }
}
