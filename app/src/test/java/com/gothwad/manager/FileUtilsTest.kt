package com.gothwad.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

class FileUtilsTest {
    @get:Rule
    val temporaryFolder = TemporaryFolder()

    @Test
    fun newestUploadIsListedFirst() {
        val directory = temporaryFolder.newFolder("uploads")
        val older = File(directory, "older.apk")
        val newest = File(directory, "newest.apk")
        assertTrue(older.createNewFile())
        assertTrue(newest.createNewFile())
        assertTrue(older.setLastModified(1000L))
        assertTrue(newest.setLastModified(2000L))

        val files = FileUtils.listUploadsForDirectory(directory)

        assertEquals(2, files.size)
        assertEquals("newest.apk", files[0].name)
        assertEquals("older.apk", files[1].name)
    }

    @Test
    fun pasteCopiesFileAndAvoidsDuplicateName() {
        val directory = temporaryFolder.newFolder("files")
        val source = File(directory, "movie.mp4")
        val output = FileOutputStream(source)
        output.write(byteArrayOf(1, 2, 3))
        output.close()

        val destination = FileUtils.paste(source, directory, false)

        assertEquals("movie (1).mp4", destination.name)
        assertTrue(source.exists())
        assertTrue(destination.exists())
        assertEquals(3L, destination.length())
    }

    @Test
    fun pasteCopiesDirectoryRecursivelyAndKeepsDottedName() {
        val parent = temporaryFolder.newFolder("parent")
        val source = File(parent, "archive.dir")
        assertTrue(source.mkdir())
        assertTrue(File(source, "inside.txt").createNewFile())

        val destination = FileUtils.paste(source, parent, false)

        assertEquals("archive.dir (1)", destination.name)
        assertTrue(File(destination, "inside.txt").exists())
    }

    @Test
    fun pasteMovesItemToAnotherDirectory() {
        val sourceDirectory = temporaryFolder.newFolder("source")
        val targetDirectory = temporaryFolder.newFolder("target")
        val source = File(sourceDirectory, "move.txt")
        assertTrue(source.createNewFile())

        val destination = FileUtils.paste(source, targetDirectory, true)

        assertFalse(source.exists())
        assertTrue(destination.exists())
        assertEquals(targetDirectory.canonicalFile, destination.parentFile?.canonicalFile)
    }

    @Test
    fun pasteTargetUsesSelectedFolderOrSelectedFilesParent() {
        val parent = temporaryFolder.newFolder("paste-target")
        val folder = File(parent, "folder")
        val file = File(parent, "file.txt")
        assertTrue(folder.mkdir())
        assertTrue(file.createNewFile())

        assertEquals(
            folder.canonicalFile,
            FileUtils.pasteTargetForSelection(folder)?.canonicalFile
        )
        assertEquals(
            parent.canonicalFile,
            FileUtils.pasteTargetForSelection(file)?.canonicalFile
        )
    }

    @Test(expected = IOException::class)
    fun pasteRejectsFolderDestinationInsideSource() {
        val source = temporaryFolder.newFolder("tree")
        val child = File(source, "child")
        assertTrue(child.mkdir())

        FileUtils.paste(source, child, false)
    }
}
