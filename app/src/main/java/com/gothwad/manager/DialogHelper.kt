package com.gothwad.manager

import android.app.AlertDialog
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.TextView
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

object DialogHelper {

    fun showSearchDialog(context: Context, onSearch: (String) -> Unit) {
        val input = EditText(context).apply {
            isSingleLine = true
            setHint(R.string.search_hint)
        }
        val dialog = AlertDialog.Builder(context)
            .setTitle(R.string.search)
            .setView(input)
            .setPositiveButton(R.string.search) { _, _ ->
                val query = input.text.toString().trim()
                if (query.isNotEmpty()) {
                    onSearch(query)
                }
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            input.requestFocus()
            val imm = context.getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        dialog.show()
    }

    fun showSortDialog(
        context: Context,
        currentOption: FileSortOption,
        onSelected: (FileSortOption) -> Unit
    ) {
        val options = FileSortOption.values()
        val names = options.map { context.getString(it.titleRes) }.toTypedArray()
        val currentIndex = options.indexOf(currentOption).coerceAtLeast(0)

        AlertDialog.Builder(context)
            .setTitle(R.string.sort_by)
            .setSingleChoiceItems(names, currentIndex) { dialog, which ->
                dialog.dismiss()
                onSelected(options[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showCategoryDialog(
        context: Context,
        onSelected: (FileCategory) -> Unit
    ) {
        val categories = FileCategory.values()
        val names = categories.map { context.getString(it.labelRes) }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(R.string.categories)
            .setItems(names) { _, which ->
                onSelected(categories[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showDriveDialog(
        context: Context,
        drives: List<StorageDrive>,
        onSelected: (StorageDrive) -> Unit
    ) {
        val items = drives.map { "${it.name}\n${it.summaryText}" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(R.string.select_storage_drive)
            .setItems(items) { _, which ->
                onSelected(drives[which])
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    fun showPropertiesDialog(context: Context, file: File) {
        val modified = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
            .format(Date(file.lastModified()))
        val isDir = file.isDirectory

        val sb = StringBuilder()
        sb.append(context.getString(R.string.prop_name, file.name)).append("\n\n")
        sb.append(context.getString(R.string.prop_location, file.parentFile?.absolutePath ?: "")).append("\n\n")

        val typeStr = if (isDir) context.getString(R.string.folder) else FileUtils.mimeType(file)
        sb.append(context.getString(R.string.prop_type, typeStr)).append("\n\n")

        val sizeStr = if (!isDir) {
            context.getString(R.string.prop_size, FileUtils.formatSize(file.length()), file.length())
        } else {
            "Size: " + context.getString(R.string.calculating)
        }
        sb.append(sizeStr).append("\n\n")
        sb.append(context.getString(R.string.prop_modified, modified)).append("\n\n")

        val perms = context.getString(
            R.string.prop_permissions,
            if (file.canRead()) "Yes" else "No",
            if (file.canWrite()) "Yes" else "No"
        )
        sb.append(perms)

        val messageView = TextView(context).apply {
            setPadding(40, 24, 40, 16)
            textSize = 14f
            setTextColor(0xFFE2E8F0.toInt())
            text = sb.toString()
        }

        val dialog = AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.file_properties_title, file.name))
            .setView(messageView)
            .setPositiveButton(R.string.close, null)
            .create()

        dialog.show()

        if (isDir) {
            Thread {
                var totalBytes = 0L
                var fileCount = 0
                var dirCount = 0
                val stack = ArrayDeque<File>()
                stack.add(file)

                while (stack.isNotEmpty()) {
                    val cur = stack.removeFirst()
                    val subs = cur.listFiles() ?: continue
                    for (sub in subs) {
                        if (sub.isDirectory) {
                            dirCount++
                            stack.add(sub)
                        } else {
                            fileCount++
                            totalBytes += sub.length()
                        }
                    }
                }

                Handler(Looper.getMainLooper()).post {
                    val updatedSb = StringBuilder()
                    updatedSb.append(context.getString(R.string.prop_name, file.name)).append("\n\n")
                    updatedSb.append(context.getString(R.string.prop_location, file.parentFile?.absolutePath ?: "")).append("\n\n")
                    updatedSb.append(context.getString(R.string.prop_type, typeStr)).append("\n\n")
                    updatedSb.append(context.getString(R.string.prop_size, FileUtils.formatSize(totalBytes), totalBytes)).append("\n\n")
                    updatedSb.append(context.getString(R.string.prop_items, fileCount, dirCount)).append("\n\n")
                    updatedSb.append(context.getString(R.string.prop_modified, modified)).append("\n\n")
                    updatedSb.append(perms)
                    messageView.text = updatedSb.toString()
                }
            }.start()
        }
    }
}
