package com.gothwad.manager

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.KeyEvent
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.AdapterView
import android.widget.Button
import android.widget.EditText
import android.widget.ListView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException
import java.util.ArrayList
import java.util.Locale

class MainActivity : Activity() {

    companion object {
        private const val STORAGE_PERMISSION_REQUEST = 100
        private const val PREFS_NAME = "gothwad_manager_prefs"
        private const val KEY_SORT_OPTION = "sort_option"
    }

    private lateinit var prefs: SharedPreferences
    private lateinit var pathView: TextView
    private lateinit var listView: ListView
    private lateinit var adapter: FileEntryAdapter
    private lateinit var filterBadgeLayout: View
    private lateinit var filterStatusText: TextView
    private lateinit var storageDriveName: TextView
    private lateinit var storageStatsText: TextView
    private lateinit var storageProgressBar: ProgressBar
    private lateinit var batchBarLayout: View
    private lateinit var batchCountText: TextView

    private var initialDirectory: File? = null
    private var currentDirectory: File? = null
    private var clipboardFiles: MutableList<File> = ArrayList()
    private var clipboardCut: Boolean = false
    private var selectedRowView: View? = null
    private var confirmLongPressHandled: Boolean = false
    private var currentSortOption: FileSortOption = FileSortOption.NAME_ASC
    private var isFilterActive: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val savedSort = prefs.getString(KEY_SORT_OPTION, FileSortOption.NAME_ASC.name)
        currentSortOption = try {
            FileSortOption.valueOf(savedSort ?: FileSortOption.NAME_ASC.name)
        } catch (e: Exception) {
            FileSortOption.NAME_ASC
        }

        pathView = findViewById(R.id.text_path)
        listView = findViewById(R.id.file_list)
        filterBadgeLayout = findViewById(R.id.layout_filter_badge)
        filterStatusText = findViewById(R.id.text_filter_status)
        storageDriveName = findViewById(R.id.text_storage_name)
        storageStatsText = findViewById(R.id.text_storage_stats)
        storageProgressBar = findViewById(R.id.progress_storage)
        batchBarLayout = findViewById(R.id.layout_batch_bar)
        batchCountText = findViewById(R.id.text_batch_count)

        initialDirectory = Environment.getExternalStorageDirectory()
        currentDirectory = initialDirectory
        adapter = FileEntryAdapter(this, emptyList())
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.empty_view)

        setupListViewListeners()
        setupTopBarButtons()
        setupBatchBarButtons()

        requestStoragePermissionIfNeeded()
        updateStorageDriveOverview()
        refresh()

        findViewById<View>(R.id.button_search).requestFocus()
    }

    override fun onResume() {
        super.onResume()
        updateStorageDriveOverview()
        if (!isFilterActive) {
            refresh()
        }
    }

    private fun setupListViewListeners() {
        listView.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val file = adapter.getItem(position)
            if (adapter.isMultiSelectMode) {
                adapter.toggleSelection(file)
                updateBatchCount()
            } else {
                openEntry(file)
            }
        }

        listView.onItemLongClickListener = AdapterView.OnItemLongClickListener { _, _, position, _ ->
            confirmLongPressHandled = true
            val file = adapter.getItem(position)
            if (!adapter.isMultiSelectMode) {
                showSelectedEntryActions(file)
            }
            true
        }

        listView.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (selectedRowView != null && selectedRowView != view) {
                    selectedRowView?.isSelected = false
                }
                selectedRowView = view
                view?.isSelected = listView.hasFocus()
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {
                selectedRowView?.isSelected = false
                selectedRowView = null
            }
        }

        listView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                if (listView.selectedItemPosition == AdapterView.INVALID_POSITION && adapter.count > 0) {
                    listView.setSelection(0)
                } else {
                    selectedRowView?.isSelected = true
                }
            } else {
                selectedRowView?.isSelected = false
            }
        }
    }

    private fun setupTopBarButtons() {
        // Storage Switcher
        findViewById<View>(R.id.button_storage_drive).setOnClickListener {
            val drives = StorageUtils.getAvailableDrives(this)
            DialogHelper.showDriveDialog(this, drives) { selectedDrive ->
                isFilterActive = false
                filterBadgeLayout.visibility = View.GONE
                openDirectory(selectedDrive.root)
                updateStorageDriveOverview()
            }
        }

        // 1. Search
        findViewById<Button>(R.id.button_search).setOnClickListener {
            DialogHelper.showSearchDialog(this) { query ->
                performSearch(query)
            }
        }

        // 2. Sort
        findViewById<Button>(R.id.button_sort).setOnClickListener {
            DialogHelper.showSortDialog(this, currentSortOption) { newOption ->
                currentSortOption = newOption
                prefs.edit().putString(KEY_SORT_OPTION, newOption.name).apply()
                refresh()
            }
        }

        // 4. Categories Quick Access
        findViewById<Button>(R.id.button_categories).setOnClickListener {
            DialogHelper.showCategoryDialog(this) { category ->
                filterByCategory(category)
            }
        }

        // 10. Apps Manager
        findViewById<Button>(R.id.button_apps).setOnClickListener {
            startActivity(Intent(this, AppManagerActivity::class.java))
        }

        // 9. Multi-Select Toggle
        findViewById<Button>(R.id.button_multiselect).setOnClickListener {
            toggleMultiSelectMode()
        }

        // New Folder
        findViewById<Button>(R.id.button_new_folder).setOnClickListener {
            showCreateFolderDialog()
        }

        // Remote Transfer
        findViewById<Button>(R.id.button_transfer).setOnClickListener {
            startActivity(Intent(this, TransferActivity::class.java))
        }

        // Clear Filter Button
        findViewById<Button>(R.id.button_clear_filter).setOnClickListener {
            isFilterActive = false
            filterBadgeLayout.visibility = View.GONE
            refresh()
        }
    }

    private fun setupBatchBarButtons() {
        findViewById<Button>(R.id.button_batch_select_all).setOnClickListener {
            if (adapter.selectedFiles.size == adapter.count) {
                adapter.clearSelection()
            } else {
                adapter.selectAll()
            }
            updateBatchCount()
        }

        findViewById<Button>(R.id.button_batch_copy).setOnClickListener {
            batchPutOnClipboard(false)
        }

        findViewById<Button>(R.id.button_batch_move).setOnClickListener {
            batchPutOnClipboard(true)
        }

        findViewById<Button>(R.id.button_batch_zip).setOnClickListener {
            batchCompressToZip()
        }

        findViewById<Button>(R.id.button_batch_delete).setOnClickListener {
            confirmBatchDelete()
        }

        findViewById<Button>(R.id.button_batch_exit).setOnClickListener {
            toggleMultiSelectMode()
        }
    }

    private fun updateStorageDriveOverview() {
        val drives = StorageUtils.getAvailableDrives(this)
        val active = drives.firstOrNull {
            val rootPath = it.root.absolutePath
            val curPath = currentDirectory?.absolutePath ?: ""
            curPath.startsWith(rootPath)
        } ?: drives.firstOrNull()

        if (active != null) {
            storageDriveName.text = active.name
            storageProgressBar.progress = active.usedPercentage
            storageStatsText.text = "${FileUtils.formatSize(active.freeBytes)} free"
        }
    }

    private fun refresh() {
        var dir = currentDirectory
        if (dir == null || !dir.exists() || !dir.canRead()) {
            dir = FileUtils.uploadDirectory(this)
            currentDirectory = dir
        }
        pathView.text = getString(R.string.current_path, dir.absolutePath)
        val files = FileUtils.listFiles(dir)
        val sorted = FileSortOption.sort(files, currentSortOption)
        adapter.replace(sorted)
        updateStorageDriveOverview()
    }

    private fun openDirectory(directory: File?) {
        if (directory != null && directory.isDirectory && directory.canRead()) {
            currentDirectory = directory
            isFilterActive = false
            filterBadgeLayout.visibility = View.GONE
            refresh()
            listView.setSelection(0)
        } else {
            Toast.makeText(this, R.string.cannot_access_directory, Toast.LENGTH_SHORT).show()
        }
    }

    private fun navigateParent() {
        val parent = currentDirectory?.parentFile
        if (parent != null && parent.canRead()) {
            openDirectory(parent)
        } else {
            Toast.makeText(this, R.string.top_directory_reached, Toast.LENGTH_SHORT).show()
        }
    }

    private fun openEntry(file: File) {
        if (file.isDirectory) {
            openDirectory(file)
            return
        }

        val ext = file.extension.lowercase(Locale.US)

        // Built-in image viewer
        if (FileCategory.PHOTOS.matches(file)) {
            val intent = Intent(this, ImageViewerActivity::class.java).apply {
                putExtra(ImageViewerActivity.EXTRA_FILE_PATH, file.absolutePath)
            }
            startActivity(intent)
            return
        }

        // Built-in text viewer
        if (ext in setOf("txt", "log", "json", "xml", "md", "cfg", "ini", "m3u", "csv")) {
            val intent = Intent(this, TextViewerActivity::class.java).apply {
                putExtra(TextViewerActivity.EXTRA_FILE_PATH, file.absolutePath)
            }
            startActivity(intent)
            return
        }

        // ZIP Archive handling
        if (ext == "zip") {
            showZipPrompt(file)
            return
        }

        // Default handler (e.g. APK, video, audio)
        FileUtils.openFile(this, file)
    }

    private fun showZipPrompt(file: File) {
        val options = arrayOf(
            getString(R.string.extract_zip),
            getString(R.string.web_open_on_tv)
        )
        AlertDialog.Builder(this)
            .setTitle(file.name)
            .setItems(options) { _, which ->
                if (which == 0) {
                    extractZipArchive(file)
                } else {
                    FileUtils.openFile(this, file)
                }
            }
            .show()
    }

    private fun extractZipArchive(zipFile: File) {
        val parent = zipFile.parentFile ?: currentDirectory ?: return
        val folderName = zipFile.nameWithoutExtension
        val targetDir = File(parent, folderName)

        Toast.makeText(this, R.string.extracting_zip, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                val count = ZipUtils.extractZip(zipFile, targetDir)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.zip_extracted_success, targetDir.name),
                        Toast.LENGTH_LONG
                    ).show()
                    refresh()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.zip_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun performSearch(query: String) {
        val root = currentDirectory ?: Environment.getExternalStorageDirectory()
        Toast.makeText(this, "Searching for \"$query\"...", Toast.LENGTH_SHORT).show()

        Thread {
            val results = mutableListOf<File>()
            val stack = ArrayDeque<File>()
            stack.add(root)

            while (stack.isNotEmpty() && results.size < 200) {
                val cur = stack.removeFirst()
                val children = cur.listFiles() ?: continue
                for (child in children) {
                    if (child.name.contains(query, ignoreCase = true)) {
                        results.add(child)
                    }
                    if (child.isDirectory && !child.name.startsWith(".") && child.name != "Android") {
                        stack.add(child)
                    }
                }
            }

            val sorted = FileSortOption.sort(results, currentSortOption)
            runOnUiThread {
                isFilterActive = true
                filterStatusText.text = "Search: \"$query\" (${sorted.size})"
                filterBadgeLayout.visibility = View.VISIBLE
                adapter.replace(sorted)
                if (sorted.isEmpty()) {
                    Toast.makeText(this, R.string.no_search_results, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun filterByCategory(category: FileCategory) {
        if (category == FileCategory.ALL) {
            isFilterActive = false
            filterBadgeLayout.visibility = View.GONE
            refresh()
            return
        }

        val root = currentDirectory ?: Environment.getExternalStorageDirectory()
        Toast.makeText(this, "Scanning for ${getString(category.labelRes)}...", Toast.LENGTH_SHORT).show()

        Thread {
            val results = FileCategory.scanCategory(root, category, 250)
            val sorted = FileSortOption.sort(results, currentSortOption)
            runOnUiThread {
                isFilterActive = true
                filterStatusText.text = "${getString(category.labelRes)} (${sorted.size})"
                filterBadgeLayout.visibility = View.VISIBLE
                adapter.replace(sorted)
                if (sorted.isEmpty()) {
                    Toast.makeText(this, "No ${getString(category.labelRes)} found", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun toggleMultiSelectMode() {
        val next = !adapter.isMultiSelectMode
        adapter.setMultiSelectMode(next)
        batchBarLayout.visibility = if (next) View.VISIBLE else View.GONE
        if (next) {
            updateBatchCount()
            batchBarLayout.requestFocus()
        }
    }

    private fun updateBatchCount() {
        val count = adapter.selectedFiles.size
        batchCountText.text = getString(R.string.selected_count, count)
        findViewById<Button>(R.id.button_batch_delete).text = getString(R.string.batch_delete, count)
        findViewById<Button>(R.id.button_batch_copy).text = getString(R.string.batch_copy, count)
        findViewById<Button>(R.id.button_batch_move).text = getString(R.string.batch_move, count)
        findViewById<Button>(R.id.button_batch_zip).text = getString(R.string.batch_zip, count)
    }

    private fun batchPutOnClipboard(cut: Boolean) {
        val selected = adapter.selectedFiles.toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
            return
        }
        clipboardFiles.clear()
        clipboardFiles.addAll(selected)
        clipboardCut = cut
        val msg = if (cut) getString(R.string.batch_cut, selected.size) else getString(R.string.batch_copied, selected.size)
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
        toggleMultiSelectMode()
    }

    private fun batchCompressToZip() {
        val selected = adapter.selectedFiles.toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
            return
        }
        val parent = currentDirectory ?: return
        val destZip = File(parent, "Archive_${System.currentTimeMillis() % 10000}.zip")

        Toast.makeText(this, R.string.compressing_zip, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                ZipUtils.compressFilesToZip(selected, destZip)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.zip_compressed_success, destZip.name),
                        Toast.LENGTH_LONG
                    ).show()
                    toggleMultiSelectMode()
                    refresh()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.zip_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun confirmBatchDelete() {
        val selected = adapter.selectedFiles.toList()
        if (selected.isEmpty()) {
            Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
            return
        }

        AlertDialog.Builder(this)
            .setTitle(R.string.permanent_delete)
            .setMessage(getString(R.string.batch_delete_confirm, selected.size))
            .setPositiveButton(R.string.permanent_delete) { _, _ ->
                Thread {
                    var deletedCount = 0
                    for (file in selected) {
                        if (FileUtils.deleteRecursively(file)) {
                            deletedCount++
                        }
                    }
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.batch_delete_success, deletedCount),
                            Toast.LENGTH_SHORT
                        ).show()
                        toggleMultiSelectMode()
                        refresh()
                    }
                }.start()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showSelectedEntryActions(file: File? = null) {
        val selected = file ?: run {
            val position = listView.selectedItemPosition
            if (position == AdapterView.INVALID_POSITION || position >= adapter.count) {
                Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
                return
            }
            adapter.getItem(position)
        }

        val actions = ArrayList<String>()
        actions.add(getString(R.string.copy))
        actions.add(getString(R.string.cut))
        if (clipboardFiles.isNotEmpty()) {
            actions.add(getString(R.string.paste))
        }
        actions.add(getString(R.string.rename))
        actions.add(getString(R.string.properties))
        if (selected.name.lowercase(Locale.US).endsWith(".zip")) {
            actions.add(getString(R.string.extract_zip))
        } else {
            actions.add(getString(R.string.compress_to_zip))
        }
        actions.add(getString(R.string.permanent_delete))

        AlertDialog.Builder(this)
            .setTitle(selected.name)
            .setItems(actions.toTypedArray()) { _, which ->
                when (actions[which]) {
                    getString(R.string.copy) -> {
                        clipboardFiles.clear()
                        clipboardFiles.add(selected)
                        clipboardCut = false
                        Toast.makeText(this, getString(R.string.copied_to_clipboard, selected.name), Toast.LENGTH_SHORT).show()
                    }
                    getString(R.string.cut) -> {
                        clipboardFiles.clear()
                        clipboardFiles.add(selected)
                        clipboardCut = true
                        Toast.makeText(this, getString(R.string.cut_to_clipboard, selected.name), Toast.LENGTH_SHORT).show()
                    }
                    getString(R.string.paste) -> pasteClipboard(selected)
                    getString(R.string.rename) -> renameEntry(selected)
                    getString(R.string.properties) -> DialogHelper.showPropertiesDialog(this, selected)
                    getString(R.string.extract_zip) -> extractZipArchive(selected)
                    getString(R.string.compress_to_zip) -> compressSingleFile(selected)
                    getString(R.string.permanent_delete) -> deleteEntry(selected)
                }
            }
            .show()
    }

    private fun compressSingleFile(file: File) {
        val parent = file.parentFile ?: currentDirectory ?: return
        val destZip = File(parent, "${file.nameWithoutExtension}.zip")
        Toast.makeText(this, R.string.compressing_zip, Toast.LENGTH_SHORT).show()
        Thread {
            try {
                ZipUtils.compressFilesToZip(listOf(file), destZip)
                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.zip_compressed_success, destZip.name),
                        Toast.LENGTH_LONG
                    ).show()
                    refresh()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.zip_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun pasteClipboard(target: File?) {
        if (clipboardFiles.isEmpty()) return
        val destDir = FileUtils.pasteTargetForSelection(target) ?: currentDirectory ?: return

        Thread {
            var pastedCount = 0
            for (clip in ArrayList(clipboardFiles)) {
                if (!clip.exists()) continue
                try {
                    if (clip.isDirectory && FileUtils.isSameOrDescendant(clip, destDir)) {
                        continue
                    }
                    FileUtils.paste(clip, destDir, clipboardCut)
                    pastedCount++
                } catch (e: IOException) {
                    // Continue with next
                }
            }
            if (clipboardCut) {
                clipboardFiles.clear()
            }
            runOnUiThread {
                Toast.makeText(
                    this,
                    getString(R.string.paste_success, destDir.name),
                    Toast.LENGTH_SHORT
                ).show()
                refresh()
            }
        }.start()
    }

    private fun renameEntry(file: File) {
        val input = EditText(this).apply {
            isSingleLine = true
            setText(file.name)
            setSelection(file.name.length)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.rename)
            .setView(input)
            .setPositiveButton(R.string.confirm) { _, _ ->
                val name = input.text.toString().trim()
                if (name.isEmpty() || name == "." || name == ".." || name.contains("/") || name.contains("\\")) {
                    Toast.makeText(this, R.string.invalid_name, Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }
                val parent = file.parentFile ?: return@setPositiveButton
                val destination = File(parent, name)
                if (destination.exists()) {
                    Toast.makeText(this, R.string.file_exists, Toast.LENGTH_SHORT).show()
                } else if (!file.renameTo(destination)) {
                    Toast.makeText(this, R.string.rename_failed, Toast.LENGTH_SHORT).show()
                }
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        dialog.show()
    }

    private fun deleteEntry(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permanent_delete)
            .setMessage(getString(R.string.delete_confirm, file.name))
            .setPositiveButton(R.string.permanent_delete) { _, _ ->
                if (!FileUtils.deleteRecursively(file)) {
                    Toast.makeText(this, R.string.delete_failed_detailed, Toast.LENGTH_LONG).show()
                }
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun showCreateFolderDialog() {
        val input = EditText(this).apply {
            isSingleLine = true
            setHint(R.string.folder_name_hint)
        }
        val dialog = AlertDialog.Builder(this)
            .setTitle(R.string.new_folder)
            .setView(input)
            .setPositiveButton(R.string.create) { _, _ ->
                val name = input.text.toString().trim()
                val current = currentDirectory
                if (current == null || name.isEmpty() || name.contains("/") || name.contains("\\")) {
                    Toast.makeText(this, R.string.invalid_simple_name, Toast.LENGTH_LONG).show()
                    return@setPositiveButton
                }
                val folder = File(current, name)
                if (folder.exists()) {
                    Toast.makeText(this, R.string.file_or_folder_exists, Toast.LENGTH_SHORT).show()
                } else if (!folder.mkdir()) {
                    Toast.makeText(this, R.string.create_folder_failed, Toast.LENGTH_LONG).show()
                }
                refresh()
            }
            .setNegativeButton(R.string.cancel, null)
            .create()

        dialog.setOnShowListener {
            input.requestFocus()
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as? InputMethodManager
            imm?.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT)
        }
        dialog.show()
    }

    private fun requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (!Environment.isExternalStorageManager()) {
                try {
                    val intent = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION).apply {
                        data = Uri.parse("package:$packageName")
                    }
                    startActivity(intent)
                    Toast.makeText(this, R.string.allow_all_files_access, Toast.LENGTH_LONG).show()
                } catch (e: Exception) {
                    try {
                        val intent = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        startActivity(intent)
                    } catch (ignored: Exception) {}
                }
            }
        } else if (Build.VERSION.SDK_INT >= 23 &&
            checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                STORAGE_PERMISSION_REQUEST
            )
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            refresh()
            if (grantResults.isEmpty() || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show()
                openDirectory(FileUtils.uploadDirectory(this))
            }
        }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        val contextMenuKey = RemoteKeyPolicy.isContextMenuKey(keyCode)
        if (contextMenuKey) {
            if (RemoteKeyPolicy.shouldOpenEntryActions(event.action, keyCode, event.repeatCount)) {
                showSelectedEntryActions()
            }
            return true
        }
        if (RemoteKeyPolicy.isConfirmKey(keyCode)) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                if (event.repeatCount == 0) {
                    confirmLongPressHandled = false
                } else if (RemoteKeyPolicy.shouldHandleConfirmLongPress(event.action, keyCode, event.repeatCount)) {
                    if (!confirmLongPressHandled) {
                        showSelectedEntryActions()
                        confirmLongPressHandled = true
                    }
                    return true
                }
            } else if (event.action == KeyEvent.ACTION_UP && confirmLongPressHandled) {
                confirmLongPressHandled = false
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        if (adapter.isMultiSelectMode) {
            toggleMultiSelectMode()
            return
        }
        if (isFilterActive) {
            isFilterActive = false
            filterBadgeLayout.visibility = View.GONE
            refresh()
            return
        }
        val external = Environment.getExternalStorageDirectory()
        val current = currentDirectory
        val parent = current?.parentFile
        if (current != null && external != null &&
            current.absolutePath != external.absolutePath &&
            parent != null && parent.canRead()
        ) {
            navigateParent()
        } else {
            super.onBackPressed()
        }
    }
}
