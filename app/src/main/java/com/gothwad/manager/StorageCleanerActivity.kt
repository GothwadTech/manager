package com.gothwad.manager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.os.Environment
import android.view.View
import android.widget.*
import java.io.File
import java.util.ArrayDeque

class StorageCleanerActivity : Activity() {

    private lateinit var statusText: TextView
    private lateinit var progressBar: ProgressBar
    private lateinit var cleanAllButton: Button
    private lateinit var tempSizeText: TextView
    private lateinit var tempCountText: TextView
    private lateinit var emptyDirsText: TextView
    private lateinit var apksSizeText: TextView
    private lateinit var apksCountText: TextView
    private lateinit var largeSizeText: TextView
    private lateinit var largeCountText: TextView
    private lateinit var largeFilesList: ListView

    private val tempFiles = mutableListOf<File>()
    private val emptyDirs = mutableListOf<File>()
    private val apkFiles = mutableListOf<File>()
    private val largeFiles = mutableListOf<File>()
    private var totalJunkBytes = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_storage_cleaner)

        statusText = findViewById(R.id.text_cleaner_status)
        progressBar = findViewById(R.id.cleaner_progress)
        cleanAllButton = findViewById(R.id.button_clean_all)
        tempSizeText = findViewById(R.id.text_temp_size)
        tempCountText = findViewById(R.id.text_temp_count)
        emptyDirsText = findViewById(R.id.text_empty_dirs_count)
        apksSizeText = findViewById(R.id.text_apks_size)
        apksCountText = findViewById(R.id.text_apks_count)
        largeSizeText = findViewById(R.id.text_large_size)
        largeCountText = findViewById(R.id.text_large_count)
        largeFilesList = findViewById(R.id.list_large_files)

        findViewById<Button>(R.id.button_cleaner_back).setOnClickListener { finish() }

        cleanAllButton.setOnClickListener {
            performClean()
        }

        startScan()
    }

    private fun startScan() {
        progressBar.visibility = View.VISIBLE
        cleanAllButton.isEnabled = false
        statusText.text = getString(R.string.scanning_storage)

        Thread {
            tempFiles.clear()
            emptyDirs.clear()
            apkFiles.clear()
            largeFiles.clear()
            totalJunkBytes = 0L

            val root = Environment.getExternalStorageDirectory() ?: filesDir
            val queue = ArrayDeque<File>()
            queue.add(root)

            val largeThreshold = 100L * 1024 * 1024 // 100MB

            while (queue.isNotEmpty()) {
                val current = queue.removeFirst()
                val children = current.listFiles()

                if (children == null || children.isEmpty()) {
                    if (current != root && !current.name.startsWith(".")) {
                        emptyDirs.add(current)
                    }
                    continue
                }

                for (child in children) {
                    val name = child.name.lowercase()
                    if (name == "android" || name.startsWith(".")) continue

                    if (child.isDirectory) {
                        queue.add(child)
                    } else {
                        val len = child.length()
                        if (name.endsWith(".tmp") || name.endsWith(".log") || name.endsWith(".cache") || name == "thumbs.db") {
                            tempFiles.add(child)
                            totalJunkBytes += len
                        } else if (name.endsWith(".apk")) {
                            apkFiles.add(child)
                            totalJunkBytes += len
                        }
                        if (len >= largeThreshold) {
                            largeFiles.add(child)
                        }
                    }
                }
            }

            runOnUiThread {
                progressBar.visibility = View.GONE
                updateUi()
            }
        }.start()
    }

    private fun updateUi() {
        val tempBytes = tempFiles.sumOf { it.length() }
        val apksBytes = apkFiles.sumOf { it.length() }
        val largeBytes = largeFiles.sumOf { it.length() }

        tempSizeText.text = FileUtils.formatSize(tempBytes)
        tempCountText.text = "${tempFiles.size} files"

        emptyDirsText.text = "${emptyDirs.size} folders"

        apksSizeText.text = FileUtils.formatSize(apksBytes)
        apksCountText.text = "${apkFiles.size} APKs"

        largeSizeText.text = FileUtils.formatSize(largeBytes)
        largeCountText.text = "${largeFiles.size} files"

        statusText.text = getString(R.string.junk_found, FileUtils.formatSize(totalJunkBytes))
        cleanAllButton.isEnabled = totalJunkBytes > 0 || emptyDirs.isNotEmpty()

        val adapter = FileEntryAdapter(this, largeFiles)
        largeFilesList.adapter = adapter
        largeFilesList.setOnItemClickListener { _, _, position, _ ->
            val file = adapter.getItem(position)
            AlertDialog.Builder(this)
                .setTitle(file.name)
                .setMessage("Size: ${FileUtils.formatSize(file.length())}\nPath: ${file.absolutePath}")
                .setPositiveButton("Open") { _, _ -> FileUtils.openFile(this, file) }
                .setNeutralButton("Delete") { _, _ ->
                    if (FileUtils.deleteRecursively(file)) {
                        largeFiles.remove(file)
                        updateUi()
                    }
                }
                .setNegativeButton(R.string.cancel, null)
                .show()
        }

        cleanAllButton.requestFocus()
    }

    private fun performClean() {
        AlertDialog.Builder(this)
            .setTitle(R.string.clean_junk)
            .setMessage("Clean ${FileUtils.formatSize(totalJunkBytes)} of temporary files and ${emptyDirs.size} empty folders?")
            .setPositiveButton(R.string.clean_now) { _, _ ->
                Thread {
                    var cleaned = 0L
                    for (file in tempFiles) {
                        val len = file.length()
                        if (file.delete()) cleaned += len
                    }
                    for (dir in emptyDirs) {
                        dir.delete()
                    }
                    for (apk in apkFiles) {
                        val len = apk.length()
                        if (apk.delete()) cleaned += len
                    }

                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.cleaning_done, FileUtils.formatSize(cleaned)),
                            Toast.LENGTH_LONG
                        ).show()
                        startScan()
                    }
                }.start()
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }
}
