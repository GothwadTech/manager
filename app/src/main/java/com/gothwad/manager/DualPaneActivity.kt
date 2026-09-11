package com.gothwad.manager

import android.app.Activity
import android.os.Bundle
import android.os.Environment
import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.IOException

class DualPaneActivity : Activity() {

    private enum class PaneType { LEFT, RIGHT }

    private var activePane = PaneType.LEFT

    private lateinit var textLeftPath: TextView
    private lateinit var textRightPath: TextView
    private lateinit var listLeft: ListView
    private lateinit var listRight: ListView
    private lateinit var containerLeft: View
    private lateinit var containerRight: View

    private lateinit var adapterLeft: FileEntryAdapter
    private lateinit var adapterRight: FileEntryAdapter

    private var currentLeftDir: File = Environment.getExternalStorageDirectory()
    private var currentRightDir: File = File(Environment.getExternalStorageDirectory(), "Download").let {
        if (it.exists()) it else Environment.getExternalStorageDirectory()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dual_pane)

        textLeftPath = findViewById(R.id.text_left_path)
        textRightPath = findViewById(R.id.text_right_path)
        listLeft = findViewById(R.id.list_left_pane)
        listRight = findViewById(R.id.list_right_pane)
        containerLeft = findViewById(R.id.container_left_pane)
        containerRight = findViewById(R.id.container_right_pane)

        adapterLeft = FileEntryAdapter(this, emptyList())
        adapterRight = FileEntryAdapter(this, emptyList())
        listLeft.adapter = adapterLeft
        listRight.adapter = adapterRight

        listLeft.setOnItemClickListener { _, _, pos, _ ->
            activePane = PaneType.LEFT
            updateFocusVisuals()
            openFileOrDir(adapterLeft.getItem(pos), PaneType.LEFT)
        }

        listRight.setOnItemClickListener { _, _, pos, _ ->
            activePane = PaneType.RIGHT
            updateFocusVisuals()
            openFileOrDir(adapterRight.getItem(pos), PaneType.RIGHT)
        }

        listLeft.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activePane = PaneType.LEFT
                updateFocusVisuals()
            }
        }

        listRight.setOnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                activePane = PaneType.RIGHT
                updateFocusVisuals()
            }
        }

        findViewById<Button>(R.id.button_dual_copy).setOnClickListener {
            transferSelected(isMove = false)
        }

        findViewById<Button>(R.id.button_dual_move).setOnClickListener {
            transferSelected(isMove = true)
        }

        findViewById<Button>(R.id.button_dual_exit).setOnClickListener {
            finish()
        }

        refreshPane(PaneType.LEFT)
        refreshPane(PaneType.RIGHT)
        updateFocusVisuals()
        listLeft.requestFocus()
    }

    private fun updateFocusVisuals() {
        if (activePane == PaneType.LEFT) {
            containerLeft.setBackgroundResource(R.drawable.card_panel)
            containerRight.alpha = 0.7f
            containerLeft.alpha = 1.0f
        } else {
            containerRight.setBackgroundResource(R.drawable.card_panel)
            containerLeft.alpha = 0.7f
            containerRight.alpha = 1.0f
        }
    }

    private fun refreshPane(pane: PaneType) {
        if (pane == PaneType.LEFT) {
            textLeftPath.text = currentLeftDir.absolutePath
            val files = FileUtils.listFiles(currentLeftDir)
            adapterLeft.replace(FileSortOption.sort(files, FileSortOption.NAME_ASC))
        } else {
            textRightPath.text = currentRightDir.absolutePath
            val files = FileUtils.listFiles(currentRightDir)
            adapterRight.replace(FileSortOption.sort(files, FileSortOption.NAME_ASC))
        }
    }

    private fun openFileOrDir(file: File, pane: PaneType) {
        if (file.isDirectory) {
            if (pane == PaneType.LEFT) {
                currentLeftDir = file
                refreshPane(PaneType.LEFT)
            } else {
                currentRightDir = file
                refreshPane(PaneType.RIGHT)
            }
        } else {
            FileUtils.openFile(this, file)
        }
    }

    private fun transferSelected(isMove: Boolean) {
        val (srcList, srcDir, destDir, destPane) = if (activePane == PaneType.LEFT) {
            Tuple4(listLeft, currentLeftDir, currentRightDir, PaneType.RIGHT)
        } else {
            Tuple4(listRight, currentRightDir, currentLeftDir, PaneType.LEFT)
        }

        val pos = srcList.selectedItemPosition
        val adapter = if (activePane == PaneType.LEFT) adapterLeft else adapterRight
        if (pos == ListView.INVALID_POSITION || pos >= adapter.count) {
            Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show()
            return
        }

        val file = adapter.getItem(pos)
        if (FileUtils.sameFile(srcDir, destDir)) {
            Toast.makeText(this, "Source and destination folders are the same", Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                FileUtils.paste(file, destDir, isMove)
                runOnUiThread {
                    val actionName = if (isMove) "Moved" else "Copied"
                    Toast.makeText(this, "$actionName ${file.name} to ${destDir.name}", Toast.LENGTH_SHORT).show()
                    refreshPane(PaneType.LEFT)
                    refreshPane(PaneType.RIGHT)
                }
            } catch (e: IOException) {
                runOnUiThread {
                    Toast.makeText(this, "Transfer failed: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT && activePane == PaneType.RIGHT) {
                activePane = PaneType.LEFT
                updateFocusVisuals()
                listLeft.requestFocus()
                return true
            }
            if (event.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT && activePane == PaneType.LEFT) {
                activePane = PaneType.RIGHT
                updateFocusVisuals()
                listRight.requestFocus()
                return true
            }
        }
        return super.dispatchKeyEvent(event)
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        val current = if (activePane == PaneType.LEFT) currentLeftDir else currentRightDir
        val parent = current.parentFile
        val root = Environment.getExternalStorageDirectory()

        if (parent != null && parent.canRead() && current.absolutePath != root.absolutePath) {
            if (activePane == PaneType.LEFT) {
                currentLeftDir = parent
                refreshPane(PaneType.LEFT)
            } else {
                currentRightDir = parent
                refreshPane(PaneType.RIGHT)
            }
        } else {
            super.onBackPressed()
        }
    }

    private data class Tuple4<A, B, C, D>(val a: A, val b: B, val c: C, val d: D)
}
