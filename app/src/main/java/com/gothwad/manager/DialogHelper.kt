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

    // 1. Recycle Bin Dialog
    fun showTrashDialog(context: Context, onRefresh: () -> Unit) {
        val items = TrashManager.getTrashItems(context)
        if (items.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle(R.string.recycle_bin)
                .setMessage(R.string.trash_empty)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }

        val itemNames = items.map {
            "${it.file.name.substringAfter("_")} (${FileUtils.formatSize(it.file.length())})"
        }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("${context.getString(R.string.recycle_bin)} (${items.size})")
            .setItems(itemNames) { _, which ->
                val chosen = items[which]
                val df = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT)
                val trashedDate = df.format(Date(chosen.trashedAt))

                AlertDialog.Builder(context)
                    .setTitle(chosen.file.name.substringAfter("_"))
                    .setMessage("Original Path: ${chosen.originalPath}\nDeleted: $trashedDate\nSize: ${FileUtils.formatSize(chosen.file.length())}")
                    .setPositiveButton(R.string.restore) { _, _ ->
                        if (TrashManager.restoreItem(context, chosen)) {
                            android.widget.Toast.makeText(
                                context,
                                context.getString(R.string.restore_success, chosen.file.name.substringAfter("_")),
                                android.widget.Toast.LENGTH_SHORT
                            ).show()
                            onRefresh()
                        }
                    }
                    .setNeutralButton(R.string.permanent_delete) { _, _ ->
                        TrashManager.deletePermanently(context, chosen)
                        onRefresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setNeutralButton(R.string.empty_trash) { _, _ ->
                AlertDialog.Builder(context)
                    .setTitle(R.string.empty_trash)
                    .setMessage(R.string.empty_trash_confirm)
                    .setPositiveButton(R.string.confirm) { _, _ ->
                        TrashManager.emptyTrash(context)
                        onRefresh()
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    // 2. Bookmarks Dialog
    fun showBookmarksDialog(context: Context, onOpenFolder: (File) -> Unit, onRefresh: () -> Unit) {
        val bookmarks = BookmarkManager.getBookmarks(context)
        if (bookmarks.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle(R.string.bookmarks)
                .setMessage(R.string.no_bookmarks)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }

        val names = bookmarks.map { "⭐ ${it.name} (${it.path})" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle(R.string.bookmarks)
            .setItems(names) { _, which ->
                val folder = bookmarks[which]
                val options = arrayOf("Open Folder", "Remove Bookmark")
                AlertDialog.Builder(context)
                    .setTitle(folder.name)
                    .setItems(options) { _, opt ->
                        if (opt == 0) {
                            onOpenFolder(folder)
                        } else {
                            BookmarkManager.removeBookmark(context, folder)
                            android.widget.Toast.makeText(context, R.string.bookmark_removed, android.widget.Toast.LENGTH_SHORT).show()
                            onRefresh()
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    // 3. Vault (Private Safe) Dialog
    fun showVaultDialog(context: Context, onRefresh: () -> Unit) {
        if (!VaultManager.isPinSet(context)) {
            // Set new PIN
            val input = EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setHint("4-digit PIN (e.g. 1234)")
                transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.set_pin)
                .setView(input)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val pin = input.text.toString().trim()
                    if (pin.length >= 4) {
                        VaultManager.setPin(context, pin)
                        android.widget.Toast.makeText(context, R.string.pin_set_success, android.widget.Toast.LENGTH_SHORT).show()
                        openVaultFiles(context, onRefresh)
                    } else {
                        android.widget.Toast.makeText(context, "PIN must be at least 4 digits", android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        } else {
            // Enter PIN
            val input = EditText(context).apply {
                inputType = android.text.InputType.TYPE_CLASS_NUMBER or android.text.InputType.TYPE_NUMBER_VARIATION_PASSWORD
                setHint("Enter 4-digit PIN")
                transformationMethod = android.text.method.PasswordTransformationMethod.getInstance()
            }
            AlertDialog.Builder(context)
                .setTitle(R.string.enter_pin)
                .setView(input)
                .setPositiveButton(R.string.confirm) { _, _ ->
                    val pin = input.text.toString().trim()
                    if (VaultManager.verifyPin(context, pin)) {
                        openVaultFiles(context, onRefresh)
                    } else {
                        android.widget.Toast.makeText(context, R.string.wrong_pin, android.widget.Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }
    }

    private fun openVaultFiles(context: Context, onRefresh: () -> Unit) {
        val files = VaultManager.getVaultFiles(context)
        if (files.isEmpty()) {
            AlertDialog.Builder(context)
                .setTitle(R.string.private_safe)
                .setMessage(R.string.vault_empty)
                .setPositiveButton(R.string.close, null)
                .show()
            return
        }

        val names = files.map { "🔒 ${it.name} (${FileUtils.formatSize(it.length())})" }.toTypedArray()
        AlertDialog.Builder(context)
            .setTitle("${context.getString(R.string.private_safe)} (${files.size})")
            .setItems(names) { _, which ->
                val file = files[which]
                val opts = arrayOf("Open File", "Move Out of Safe (Restore)", "Delete Forever")
                AlertDialog.Builder(context)
                    .setTitle(file.name)
                    .setItems(opts) { _, opt ->
                        when (opt) {
                            0 -> FileUtils.openFile(context, file)
                            1 -> {
                                VaultManager.restoreFromVault(context, file)
                                android.widget.Toast.makeText(context, "File restored to Downloads", android.widget.Toast.LENGTH_SHORT).show()
                                onRefresh()
                            }
                            2 -> {
                                FileUtils.deleteRecursively(file)
                                onRefresh()
                            }
                        }
                    }
                    .setNegativeButton(R.string.cancel, null)
                    .show()
            }
            .setPositiveButton(R.string.close, null)
            .show()
    }

    // 4. FTP Server Dialog
    fun showFtpDialog(context: Context, ftpServer: SimpleFtpServer) {
        val ip = StorageUtils.findLocalIpv4() ?: "127.0.0.1"
        val isRunning = ftpServer.isServerRunning()
        val status = if (isRunning) context.getString(R.string.ftp_running) else context.getString(R.string.ftp_stopped)
        val url = context.getString(R.string.ftp_address, ip, 2121)

        val msg = "$status\n\nURL: $url\n\n${context.getString(R.string.ftp_guide)}"

        val builder = AlertDialog.Builder(context)
            .setTitle(R.string.ftp_server)
            .setMessage(msg)
            .setPositiveButton(R.string.close, null)

        if (isRunning) {
            builder.setNegativeButton(R.string.stop_ftp) { _, _ ->
                ftpServer.stop()
                android.widget.Toast.makeText(context, R.string.ftp_stopped, android.widget.Toast.LENGTH_SHORT).show()
            }
        } else {
            builder.setNeutralButton(R.string.start_ftp) { _, _ ->
                if (ftpServer.start()) {
                    android.widget.Toast.makeText(context, "FTP Server started at $url", android.widget.Toast.LENGTH_LONG).show()
                } else {
                    android.widget.Toast.makeText(context, "Failed to start FTP server", android.widget.Toast.LENGTH_SHORT).show()
                }
            }
        }
        builder.show()
    }

    // 5. LAN Scanner Dialog
    fun showLanScannerDialog(context: Context) {
        val progressDialog = AlertDialog.Builder(context)
            .setTitle(R.string.lan_scanner)
            .setMessage(context.getString(R.string.scanning_network))
            .setCancelable(false)
            .show()

        LanScanner.scanSubnet(context, { done, total ->
            Handler(Looper.getMainLooper()).post {
                progressDialog.setMessage("${context.getString(R.string.scanning_network)} ($done/$total)")
            }
        }, { devices ->
            Handler(Looper.getMainLooper()).post {
                progressDialog.dismiss()
                if (devices.isEmpty()) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.lan_scanner)
                        .setMessage(R.string.no_devices_found)
                        .setPositiveButton(R.string.close, null)
                        .show()
                } else {
                    val list = devices.map { dev ->
                        val services = mutableListOf<String>()
                        if (dev.openPorts.contains(445)) services.add("SMB/Windows")
                        if (dev.openPorts.contains(80)) services.add("HTTP")
                        if (dev.openPorts.contains(21)) services.add("FTP")
                        if (dev.openPorts.contains(8080)) services.add("Web:8080")
                        val serviceStr = if (services.isNotEmpty()) " [${services.joinToString(", ")}]" else ""
                        "🖥️ ${dev.ip} - ${dev.hostname}$serviceStr"
                    }.toTypedArray()

                    AlertDialog.Builder(context)
                        .setTitle(context.getString(R.string.devices_found, devices.size))
                        .setItems(list, null)
                        .setPositiveButton(R.string.close, null)
                        .show()
                }
            }
        })
    }

    // 6. Recent Files Dialog
    fun showRecentFilesDialog(context: Context, onOpenFile: (File) -> Unit) {
        Thread {
            val recents = RecentFilesManager.scanRecentFiles()
            Handler(Looper.getMainLooper()).post {
                if (recents.isEmpty()) {
                    AlertDialog.Builder(context)
                        .setTitle(R.string.recent_files)
                        .setMessage(R.string.no_recent_files)
                        .setPositiveButton(R.string.close, null)
                        .show()
                } else {
                    val df = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                    val names = recents.map {
                        "${it.name} (${FileUtils.formatSize(it.length())} · ${df.format(Date(it.lastModified()))})"
                    }.toTypedArray()

                    AlertDialog.Builder(context)
                        .setTitle("${context.getString(R.string.recent_files)} (${recents.size})")
                        .setItems(names) { _, which ->
                            onOpenFile(recents[which])
                        }
                        .setPositiveButton(R.string.close, null)
                        .show()
                }
            }
        }.start()
    }
}
