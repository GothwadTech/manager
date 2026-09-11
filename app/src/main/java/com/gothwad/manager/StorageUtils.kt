package com.gothwad.manager

import android.content.Context
import android.os.Environment
import android.os.StatFs
import java.io.File
import java.util.Locale

data class StorageDrive(
    val name: String,
    val root: File,
    val totalBytes: Long,
    val freeBytes: Long,
    val isRemovable: Boolean
) {
    val usedBytes: Long get() = (totalBytes - freeBytes).coerceAtLeast(0L)
    val usedPercentage: Int
        get() = if (totalBytes > 0) ((usedBytes * 100) / totalBytes).toInt().coerceIn(0, 100) else 0

    val summaryText: String
        get() {
            val free = FileUtils.formatSize(freeBytes)
            val total = FileUtils.formatSize(totalBytes)
            return "$free free of $total ($usedPercentage% used)"
        }
}

object StorageUtils {

    fun getAvailableDrives(context: Context): List<StorageDrive> {
        val drives = mutableListOf<StorageDrive>()

        // 1. Primary Internal Storage
        val internalDir = Environment.getExternalStorageDirectory()
        if (internalDir != null && internalDir.exists()) {
            val (total, free) = getStorageStats(internalDir)
            drives.add(
                StorageDrive(
                    name = context.getString(R.string.internal_storage),
                    root = internalDir,
                    totalBytes = total,
                    freeBytes = free,
                    isRemovable = false
                )
            )
        }

        // 2. Check external files dirs for secondary USB / SD Cards
        val extDirs = context.getExternalFilesDirs(null)
        val addedPaths = mutableSetOf<String>()
        internalDir?.canonicalPath?.let { addedPaths.add(it) }

        if (extDirs != null) {
            for (dir in extDirs) {
                if (dir == null) continue
                // Trace up to the mount point (typically /storage/XXXX-XXXX)
                var current: File? = dir
                var mountPoint: File? = null
                while (current != null && current.parentFile != null) {
                    val parent = current.parentFile
                    if (parent != null && parent.name == "storage") {
                        mountPoint = current
                        break
                    }
                    current = parent
                }

                val driveRoot = mountPoint ?: dir
                val canonical = try { driveRoot.canonicalPath } catch (e: Exception) { driveRoot.absolutePath }
                if (canonical !in addedPaths && driveRoot.canRead()) {
                    addedPaths.add(canonical)
                    val (total, free) = getStorageStats(driveRoot)
                    val name = if (mountPoint != null) {
                        "${context.getString(R.string.usb_storage)} (${mountPoint.name})"
                    } else {
                        context.getString(R.string.usb_storage)
                    }
                    drives.add(
                        StorageDrive(
                            name = name,
                            root = driveRoot,
                            totalBytes = total,
                            freeBytes = free,
                            isRemovable = true
                        )
                    )
                }
            }
        }

        // 3. App Uploads Directory (Guaranteed private/fallback sandbox)
        val uploads = FileUtils.uploadDirectory(context)
        val uploadsCanonical = try { uploads.canonicalPath } catch (e: Exception) { uploads.absolutePath }
        if (uploadsCanonical !in addedPaths) {
            val (total, free) = getStorageStats(uploads)
            drives.add(
                StorageDrive(
                    name = context.getString(R.string.app_uploads_folder),
                    root = uploads,
                    totalBytes = total,
                    freeBytes = free,
                    isRemovable = false
                )
            )
        }

        return drives
    }

    fun getStorageStats(file: File): Pair<Long, Long> {
        return try {
            val stat = StatFs(file.path)
            val blockSize = stat.blockSizeLong
            val totalBlocks = stat.blockCountLong
            val availableBlocks = stat.availableBlocksLong
            Pair(totalBlocks * blockSize, availableBlocks * blockSize)
        } catch (e: Exception) {
            Pair(0L, 0L)
        }
    }

    fun findLocalIpv4(): String? {
        try {
            val interfaces = java.net.NetworkInterface.getNetworkInterfaces() ?: return null
            for (network in java.util.Collections.list(interfaces)) {
                if (!network.isUp || network.isLoopback) continue
                for (address in java.util.Collections.list(network.inetAddresses)) {
                    if (address is java.net.Inet4Address && !address.isLoopbackAddress && address.isSiteLocalAddress) {
                        return address.hostAddress
                    }
                }
            }
        } catch (ignored: Exception) {}
        return null
    }
}
