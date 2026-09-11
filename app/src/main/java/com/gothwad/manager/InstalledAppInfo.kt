package com.gothwad.manager

import android.graphics.drawable.Drawable
import java.io.File

data class InstalledAppInfo(
    val appName: String,
    val packageName: String,
    val versionName: String,
    val versionCode: Long,
    val apkFile: File,
    val icon: Drawable?,
    val isSystemApp: Boolean
) {
    val apkSize: Long get() = if (apkFile.exists()) apkFile.length() else 0L
    val formattedSize: String get() = FileUtils.formatSize(apkSize)
}
