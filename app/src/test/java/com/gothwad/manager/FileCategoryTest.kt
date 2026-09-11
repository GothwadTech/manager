package com.gothwad.manager

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class FileCategoryTest {

    @Test
    fun matchesVideoExtensions() {
        assertTrue(FileCategory.VIDEOS.matches(File("movie.mp4")))
        assertTrue(FileCategory.VIDEOS.matches(File("show.MKV")))
        assertTrue(FileCategory.VIDEOS.matches(File("stream.ts")))
        assertFalse(FileCategory.VIDEOS.matches(File("song.mp3")))
        assertFalse(FileCategory.VIDEOS.matches(File("folder")))
    }

    @Test
    fun matchesAudioExtensions() {
        assertTrue(FileCategory.MUSIC.matches(File("song.mp3")))
        assertTrue(FileCategory.MUSIC.matches(File("track.flac")))
        assertTrue(FileCategory.MUSIC.matches(File("voice.m4a")))
        assertFalse(FileCategory.MUSIC.matches(File("picture.png")))
    }

    @Test
    fun matchesImageExtensions() {
        assertTrue(FileCategory.PHOTOS.matches(File("photo.jpg")))
        assertTrue(FileCategory.PHOTOS.matches(File("image.png")))
        assertTrue(FileCategory.PHOTOS.matches(File("sticker.webp")))
        assertFalse(FileCategory.PHOTOS.matches(File("app.apk")))
    }

    @Test
    fun matchesApkAndArchiveExtensions() {
        assertTrue(FileCategory.APKS.matches(File("installer.apk")))
        assertTrue(FileCategory.ARCHIVES.matches(File("backup.zip")))
        assertTrue(FileCategory.ARCHIVES.matches(File("archive.rar")))
        assertTrue(FileCategory.DOCUMENTS.matches(File("readme.txt")))
        assertTrue(FileCategory.DOCUMENTS.matches(File("manual.pdf")))
    }
}
