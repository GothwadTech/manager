package com.gothwad.manager

import android.app.Activity
import android.os.Bundle
import android.widget.Button
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import java.io.BufferedReader
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

class TextViewerActivity : Activity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
        private const val MAX_READ_BYTES = 512 * 1024 // 512 KB preview limit
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_text_viewer)

        val nameView = findViewById<TextView>(R.id.text_file_name)
        val metaView = findViewById<TextView>(R.id.text_file_meta)
        val contentView = findViewById<TextView>(R.id.text_content)
        val closeBtn = findViewById<Button>(R.id.button_close_text)
        val scrollView = findViewById<ScrollView>(R.id.scroll_view)

        closeBtn.setOnClickListener { finish() }

        val path = intent.getStringExtra(EXTRA_FILE_PATH)
        val file = if (path != null) File(path) else null

        if (file == null || !file.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        nameView.text = file.name

        try {
            val length = file.length()
            val toRead = length.coerceAtMost(MAX_READ_BYTES.toLong()).toInt()
            val buffer = CharArray(toRead)
            var readCount = 0

            BufferedReader(InputStreamReader(FileInputStream(file), StandardCharsets.UTF_8)).use { reader ->
                readCount = reader.read(buffer, 0, toRead)
            }

            val text = if (readCount > 0) String(buffer, 0, readCount) else ""
            contentView.text = text

            val lineCount = text.count { it == '\n' } + if (text.isNotEmpty()) 1 else 0
            val sizeStr = FileUtils.formatSize(length)
            metaView.text = getString(R.string.text_meta, lineCount, sizeStr)
        } catch (e: Exception) {
            contentView.text = getString(R.string.text_load_error)
            Toast.makeText(this, R.string.text_load_error, Toast.LENGTH_SHORT).show()
        }

        scrollView.requestFocus()
    }
}
