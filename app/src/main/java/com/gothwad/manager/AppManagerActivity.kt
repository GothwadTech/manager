package com.gothwad.manager

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.Button
import android.widget.ImageView
import android.widget.ListView
import android.widget.TextView
import android.widget.Toast
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.Locale

class AppManagerActivity : Activity() {

    private lateinit var countView: TextView
    private lateinit var listView: ListView
    private lateinit var emptyView: TextView
    private var appsList: List<InstalledAppInfo> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_app_manager)

        countView = findViewById(R.id.text_app_count)
        listView = findViewById(R.id.apps_list)
        emptyView = findViewById(R.id.text_empty_apps)

        findViewById<Button>(R.id.button_back_apps).setOnClickListener {
            finish()
        }

        listView.setOnItemClickListener { _, _, position, _ ->
            if (position in appsList.indices) {
                showAppActions(appsList[position])
            }
        }

        loadInstalledApps()
    }

    private fun loadInstalledApps() {
        Thread {
            val pm = packageManager
            val packages = pm.getInstalledPackages(0)
            val result = mutableListOf<InstalledAppInfo>()

            for (pkg in packages) {
                val appInfo = pkg.applicationInfo ?: continue
                // Exclude this app itself
                if (pkg.packageName == packageName) continue

                val isSystem = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0
                val label = try {
                    appInfo.loadLabel(pm).toString()
                } catch (e: Exception) {
                    pkg.packageName
                }
                val icon = try {
                    appInfo.loadIcon(pm)
                } catch (e: Exception) {
                    null
                }
                val sourceDir = appInfo.sourceDir
                val apkFile = if (sourceDir != null) File(sourceDir) else File("")

                val vCode = if (Build.VERSION.SDK_INT >= 28) {
                    pkg.longVersionCode
                } else {
                    @Suppress("DEPRECATION")
                    pkg.versionCode.toLong()
                }

                result.add(
                    InstalledAppInfo(
                        appName = label,
                        packageName = pkg.packageName,
                        versionName = pkg.versionName ?: "1.0",
                        versionCode = vCode,
                        apkFile = apkFile,
                        icon = icon,
                        isSystemApp = isSystem
                    )
                )
            }

            // User apps first, then alphabetically
            result.sortWith { a, b ->
                if (a.isSystemApp != b.isSystemApp) {
                    if (!a.isSystemApp) -1 else 1
                } else {
                    a.appName.compareTo(b.appName, ignoreCase = true)
                }
            }

            runOnUiThread {
                appsList = result
                countView.text = "${appsList.size} applications found"
                listView.adapter = AppAdapter(appsList)
                emptyView.visibility = if (appsList.isEmpty()) View.VISIBLE else View.GONE
                if (appsList.isNotEmpty()) {
                    listView.requestFocus()
                    listView.setSelection(0)
                }
            }
        }.start()
    }

    private fun showAppActions(app: InstalledAppInfo) {
        val options = arrayOf(
            getString(R.string.launch_app),
            getString(R.string.backup_apk),
            getString(R.string.uninstall_app)
        )

        AlertDialog.Builder(this)
            .setTitle(app.appName)
            .setItems(options) { _, which ->
                when (which) {
                    0 -> launchApp(app)
                    1 -> backupAppApk(app)
                    2 -> uninstallApp(app)
                }
            }
            .show()
    }

    private fun launchApp(app: InstalledAppInfo) {
        try {
            val launchIntent = packageManager.getLaunchIntentForPackage(app.packageName)
            if (launchIntent != null) {
                startActivity(launchIntent)
            } else {
                Toast.makeText(this, "Cannot launch this application directly", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(this, "Failed to launch application", Toast.LENGTH_SHORT).show()
        }
    }

    private fun backupAppApk(app: InstalledAppInfo) {
        if (!app.apkFile.exists() || !app.apkFile.canRead()) {
            Toast.makeText(this, R.string.app_backup_failed, Toast.LENGTH_SHORT).show()
            return
        }

        Thread {
            try {
                val baseDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
                val backupDir = File(baseDir, "GothwadBackups")
                if (!backupDir.exists()) backupDir.mkdirs()

                val safeName = app.appName.replace("[^a-zA-Z0-9_.-]".toRegex(), "_")
                val backupFile = File(backupDir, "${safeName}-v${app.versionName}.apk")

                FileInputStream(app.apkFile).use { input ->
                    FileOutputStream(backupFile).use { output ->
                        val buffer = ByteArray(16384)
                        var read: Int
                        while (input.read(buffer).also { read = it } != -1) {
                            output.write(buffer, 0, read)
                        }
                    }
                }

                runOnUiThread {
                    Toast.makeText(
                        this,
                        getString(R.string.app_backup_success, backupFile.absolutePath),
                        Toast.LENGTH_LONG
                    ).show()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(this, R.string.app_backup_failed, Toast.LENGTH_SHORT).show()
                }
            }
        }.start()
    }

    private fun uninstallApp(app: InstalledAppInfo) {
        try {
            val intent = Intent(Intent.ACTION_UNINSTALL_PACKAGE).apply {
                data = Uri.parse("package:${app.packageName}")
                putExtra(Intent.EXTRA_RETURN_RESULT, true)
            }
            startActivity(intent)
        } catch (e: Exception) {
            try {
                val intent = Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}"))
                startActivity(intent)
            } catch (ignored: Exception) {
                Toast.makeText(this, "Uninstall intent failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class AppAdapter(private val list: List<InstalledAppInfo>) : BaseAdapter() {
        private val inflater = LayoutInflater.from(this@AppManagerActivity)

        override fun getCount(): Int = list.size
        override fun getItem(position: Int): InstalledAppInfo = list[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            val view = convertView ?: inflater.inflate(R.layout.row_app, parent, false)
            val app = getItem(position)

            val iconView = view.findViewById<ImageView>(R.id.app_icon)
            val nameView = view.findViewById<TextView>(R.id.app_name)
            val metaView = view.findViewById<TextView>(R.id.app_package_meta)
            val sizeView = view.findViewById<TextView>(R.id.app_size)

            if (app.icon != null) {
                iconView.setImageDrawable(app.icon)
            } else {
                iconView.setImageResource(android.R.drawable.sym_def_app_icon)
            }

            nameView.text = app.appName
            val tag = if (app.isSystemApp) " [System]" else ""
            metaView.text = "${app.packageName}  ·  v${app.versionName}$tag"
            sizeView.text = app.formattedSize

            return view
        }
    }
}
