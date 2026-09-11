package com.gothwad.manager

import android.app.Activity
import android.app.AlertDialog
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import com.google.zxing.WriterException
import java.io.File
import java.io.IOException
import java.net.Inet4Address
import java.net.NetworkInterface
import java.util.Collections
import java.util.UUID

class TransferActivity : Activity(), UploadEntryAdapter.Listener, EmbeddedHttpServer.Listener {

    private lateinit var urlView: TextView
    private lateinit var pathView: TextView
    private lateinit var statusView: TextView
    private lateinit var qrView: ImageView
    private lateinit var listView: ListView
    private lateinit var adapter: UploadEntryAdapter
    private var server: EmbeddedHttpServer? = null
    private var token: String? = null
    private lateinit var uploadDirectory: File
    private var consumedNavigationKeyCode: Int = KeyEvent.KEYCODE_UNKNOWN
    private var focusFirstActionWhenWindowIsReady: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        setContentView(R.layout.activity_transfer)

        urlView = findViewById(R.id.text_server_url)
        pathView = findViewById(R.id.text_upload_path)
        statusView = findViewById(R.id.text_server_status)
        qrView = findViewById(R.id.image_qr)
        listView = findViewById(R.id.upload_list)

        // Some Android TV 4.x ListView implementations keep focus on the
        // ListView itself unless focusable adapter descendants are enabled.
        listView.itemsCanFocus = true
        listView.isFocusable = false

        uploadDirectory = FileUtils.uploadDirectory(this)
        pathView.text = getString(R.string.saved_path, uploadDirectory.absolutePath)
        adapter = UploadEntryAdapter(this, FileUtils.listUploads(this), this)
        listView.adapter = adapter
        listView.emptyView = findViewById(R.id.upload_empty_view)
    }

    override fun onStart() {
        super.onStart()
        startServer()
        refreshFiles(true)
    }

    override fun onStop() {
        stopServer()
        super.onStop()
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus && focusFirstActionWhenWindowIsReady) {
            focusFirstUploadAction()
        }
    }

    private fun startServer() {
        stopServer()
        token = UUID.randomUUID().toString().replace("-", "").substring(0, 12)
        var selectedPort = -1
        for (port in 8080..8090) {
            val candidate = EmbeddedHttpServer(this, port, uploadDirectory, token!!, this)
            try {
                candidate.start(5000, false)
                server = candidate
                selectedPort = port
                break
            } catch (ignored: IOException) {
                candidate.stop()
            }
        }

        val ip = findLocalIpv4()
        if (server == null) {
            urlView.setText(R.string.http_start_failed)
            statusView.setText(R.string.ports_unavailable)
            qrView.setImageDrawable(null)
            return
        }
        if (ip == null) {
            urlView.setText(R.string.connect_wifi_first)
            statusView.setText(R.string.no_lan_address)
            qrView.setImageDrawable(null)
            return
        }

        val url = "http://$ip:$selectedPort/?token=$token"
        urlView.text = url
        statusView.setText(R.string.server_running)
        try {
            val qr = QrCodeUtil.create(url, 420)
            qrView.setImageBitmap(qr)
        } catch (e: WriterException) {
            statusView.text = getString(R.string.qr_generation_failed, e.message)
        }
    }

    private fun stopServer() {
        server?.stop()
        server = null
    }

    private fun findLocalIpv4(): String? {
        try {
            val interfaces = NetworkInterface.getNetworkInterfaces() ?: return null
            for (network in Collections.list(interfaces)) {
                if (!network.isUp || network.isLoopback) continue
                for (address in Collections.list(network.inetAddresses)) {
                    if (address is Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }

    private fun refreshFiles(focusNewestAction: Boolean) {
        adapter.replace(FileUtils.listUploads(this))
        focusFirstActionWhenWindowIsReady = focusNewestAction && adapter.count > 0
        if (!focusFirstActionWhenWindowIsReady) return

        focusFirstUploadAction()
    }

    private fun focusFirstUploadAction() {
        if (adapter.count == 0) {
            focusFirstActionWhenWindowIsReady = false
            return
        }

        listView.setSelectionFromTop(0, 0)
        listView.post {
            listView.setSelectionFromTop(0, 0)
            listView.post {
                val firstRow = listView.getChildAt(0) ?: return@post
                val action = firstRow.findViewById<View>(R.id.upload_open)
                if (action != null && action.requestFocus()) {
                    focusFirstActionWhenWindowIsReady = false
                }
            }
        }
    }

    private fun focusUploadAction(adapterPosition: Int, actionViewId: Int) {
        val childIndex = adapterPosition - listView.firstVisiblePosition
        val row = if (childIndex >= 0) listView.getChildAt(childIndex) else null
        if (row != null) {
            val action = row.findViewById<View>(actionViewId)
            if (action != null) {
                if (!action.requestFocus()) action.requestFocusFromTouch()
                if (action.hasFocus()) return
            }
        }

        listView.setSelection(adapterPosition)
        listView.post {
            val selected = listView.selectedItemPosition
            val index = selected - listView.firstVisiblePosition
            val selectedRow = if (index >= 0) listView.getChildAt(index) else null ?: return@post
            val action = selectedRow.findViewById<View>(actionViewId)
            if (action != null && !action.requestFocus()) {
                action.requestFocusFromTouch()
            }
        }
    }

    override fun onOpen(file: File) {
        FileUtils.openFile(this, file)
    }

    override fun onDelete(file: File) {
        AlertDialog.Builder(this)
            .setTitle(R.string.permanent_delete)
            .setMessage(getString(R.string.delete_confirm, file.name))
            .setPositiveButton(R.string.permanent_delete) { _, _ ->
                if (!FileUtils.deleteRecursively(file)) {
                    Toast.makeText(this, R.string.delete_failed, Toast.LENGTH_LONG).show()
                }
                refreshFiles(false)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    override fun onFilesChanged() {
        runOnUiThread { refreshFiles(true) }
    }

    override fun onOpenRequested(file: File) {
        runOnUiThread { FileUtils.openFile(this, file) }
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val keyCode = event.keyCode
        if (event.action == KeyEvent.ACTION_UP && consumedNavigationKeyCode == keyCode) {
            consumedNavigationKeyCode = KeyEvent.KEYCODE_UNKNOWN
            return true
        }
        if (event.action == KeyEvent.ACTION_DOWN && isDpadDirection(keyCode)) {
            val focused = currentFocus
            if (adapter.count > 0 && (focused == null ||
                        (focused.id != R.id.upload_open && focused.id != R.id.upload_delete))) {
                consumedNavigationKeyCode = keyCode
                focusUploadAction(0, R.id.upload_open)
                return true
            }
        }
        if (event.action == KeyEvent.ACTION_DOWN &&
            (keyCode == KeyEvent.KEYCODE_DPAD_UP || keyCode == KeyEvent.KEYCODE_DPAD_DOWN)) {
            val focused = currentFocus
            if (focused != null &&
                (focused.id == R.id.upload_open || focused.id == R.id.upload_delete)) {
                val current = listView.getPositionForView(focused)
                val target = RemoteKeyPolicy.nextUploadPosition(current, keyCode, adapter.count)
                if (target >= 0) {
                    consumedNavigationKeyCode = keyCode
                    focusUploadAction(target, focused.id)
                    return true
                }
            }
        }
        return super.dispatchKeyEvent(event)
    }

    private fun isDpadDirection(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_UP ||
                keyCode == KeyEvent.KEYCODE_DPAD_DOWN ||
                keyCode == KeyEvent.KEYCODE_DPAD_LEFT ||
                keyCode == KeyEvent.KEYCODE_DPAD_RIGHT
    }
}
