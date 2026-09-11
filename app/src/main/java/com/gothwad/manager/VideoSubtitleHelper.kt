package com.gothwad.manager

import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import java.io.File

object VideoSubtitleHelper {

    private val SUBTITLE_EXTENSIONS = listOf("srt", "vtt", "sub", "ass", "ssa")

    fun findMatchingSubtitle(videoFile: File): File? {
        val parent = videoFile.parentFile ?: return null
        val baseName = videoFile.nameWithoutExtension.lowercase()
        val siblings = parent.listFiles() ?: return null

        // 1. Exact match e.g. "movie.srt"
        for (ext in SUBTITLE_EXTENSIONS) {
            val candidate = File(parent, "${videoFile.nameWithoutExtension}.$ext")
            if (candidate.exists() && candidate.isFile) return candidate
        }

        // 2. Language match e.g. "movie.en.srt", "movie.hi.srt"
        for (file in siblings) {
            val name = file.name.lowercase()
            val ext = file.extension.lowercase()
            if (SUBTITLE_EXTENSIONS.contains(ext) && name.startsWith(baseName)) {
                return file
            }
        }

        return null
    }

    fun playVideoWithSubtitleOption(context: Context, videoFile: File) {
        val subFile = findMatchingSubtitle(videoFile)
        if (subFile == null) {
            // Normal play
            FileUtils.openFile(context, videoFile)
            return
        }

        AlertDialog.Builder(context)
            .setTitle(context.getString(R.string.subtitles_detected, subFile.name))
            .setMessage("Found matching subtitle in this folder. How would you like to play?")
            .setPositiveButton(R.string.play_with_subtitles) { _, _ ->
                launchPlayer(context, videoFile, subFile)
            }
            .setNeutralButton(R.string.play_normal) { _, _ ->
                FileUtils.openFile(context, videoFile)
            }
            .setNegativeButton(R.string.cancel, null)
            .show()
    }

    private fun launchPlayer(context: Context, videoFile: File, subtitleFile: File) {
        try {
            val videoUri = LocalFileProvider.uriForFile(context, videoFile)
            val subUri = LocalFileProvider.uriForFile(context, subtitleFile)

            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(videoUri, "video/*")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                // VLC and MX Player subtitle extras
                putExtra("subtitles_location", subUri)
                putExtra("subs", arrayOf<android.net.Uri>(subUri))
                putExtra("subs.name", arrayOf<String>(subtitleFile.name))
                putExtra("subtitles", subUri)
            }

            context.startActivity(Intent.createChooser(intent, "Play Video"))
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open video player: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
