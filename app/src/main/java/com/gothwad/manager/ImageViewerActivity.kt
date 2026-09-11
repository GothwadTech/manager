package com.gothwad.manager

import android.app.Activity
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.util.Locale

class ImageViewerActivity : Activity() {

    companion object {
        const val EXTRA_FILE_PATH = "file_path"
    }

    private lateinit var imageView: ImageView
    private lateinit var nameView: TextView
    private lateinit var metaView: TextView
    private lateinit var topOverlay: View
    private lateinit var bottomOverlay: View

    private var imageFiles: List<File> = emptyList()
    private var currentIndex: Int = 0
    private var currentBitmap: Bitmap? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_image_viewer)

        val rootView = findViewById<View>(R.id.viewer_root)
        imageView = findViewById(R.id.image_display)
        nameView = findViewById(R.id.image_name)
        metaView = findViewById(R.id.image_meta)
        topOverlay = findViewById(R.id.top_overlay)
        bottomOverlay = findViewById(R.id.bottom_overlay)

        val path = intent.getStringExtra(EXTRA_FILE_PATH)
        val initialFile = if (path != null) File(path) else null

        if (initialFile == null || !initialFile.exists()) {
            Toast.makeText(this, R.string.file_not_found, Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        // Find all sibling images in directory
        val parent = initialFile.parentFile
        val siblings = parent?.listFiles()?.filter {
            !it.isDirectory && FileCategory.PHOTOS.matches(it)
        }?.sortedBy { it.name.lowercase(Locale.US) } ?: listOf(initialFile)

        imageFiles = if (siblings.isNotEmpty()) siblings else listOf(initialFile)
        currentIndex = imageFiles.indexOfFirst { it.absolutePath == initialFile.absolutePath }
        if (currentIndex < 0) currentIndex = 0

        loadImage(currentIndex)

        rootView.requestFocus()
    }

    private fun loadImage(index: Int) {
        if (index !in imageFiles.indices) return
        currentIndex = index
        val file = imageFiles[currentIndex]

        currentBitmap?.recycle()
        currentBitmap = null

        // Decode bounds first to prevent OOM
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(file.absolutePath, options)
        val origWidth = options.outWidth
        val origHeight = options.outHeight

        // Sample down if image is larger than 1920x1080
        val maxDim = 1920
        var sampleSize = 1
        while (options.outWidth / sampleSize > maxDim || options.outHeight / sampleSize > maxDim) {
            sampleSize *= 2
        }

        val decodeOptions = BitmapFactory.Options().apply {
            inSampleSize = sampleSize
        }
        val bitmap = BitmapFactory.decodeFile(file.absolutePath, decodeOptions)
        currentBitmap = bitmap

        imageView.setImageBitmap(bitmap)
        nameView.text = file.name

        val indexStr = "[${currentIndex + 1}/${imageFiles.size}]"
        val sizeStr = FileUtils.formatSize(file.length())
        metaView.text = "$indexStr  ·  ${origWidth}×${origHeight}  ·  $sizeStr"
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    if (currentIndex > 0) {
                        loadImage(currentIndex - 1)
                    } else {
                        loadImage(imageFiles.size - 1) // Wrap around
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT, KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    if (currentIndex < imageFiles.size - 1) {
                        loadImage(currentIndex + 1)
                    } else {
                        loadImage(0) // Wrap around
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    toggleOverlays()
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun toggleOverlays() {
        val nextVis = if (topOverlay.visibility == View.VISIBLE) View.GONE else View.VISIBLE
        topOverlay.visibility = nextVis
        bottomOverlay.visibility = nextVis
    }

    override fun onDestroy() {
        super.onDestroy()
        currentBitmap?.recycle()
        currentBitmap = null
    }
}
