package com.gothwad.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class ZipUtilsTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun compressAndExtractZipRestoresFilesAccurately() {
        val workDir = tempFolder.newFolder("work")
        val file1 = File(workDir, "test1.txt").apply {
            createNewFile()
            writeText("Hello Android TV")
        }
        val subDir = File(workDir, "sub").apply { mkdir() }
        val file2 = File(subDir, "test2.txt").apply {
            createNewFile()
            writeText("Nested file content")
        }

        val zipFile = File(tempFolder.root, "output.zip")
        val entriesCount = ZipUtils.compressFilesToZip(listOf(file1, subDir), zipFile)
        assertTrue(zipFile.exists())
        assertTrue("Entries count should be at least 3 (file1, subDir, file2)", entriesCount >= 3)

        val extractDir = tempFolder.newFolder("extracted")
        val extractedCount = ZipUtils.extractZip(zipFile, extractDir)
        assertEquals(2, extractedCount)

        val extractedFile1 = File(extractDir, "test1.txt")
        val extractedFile2 = File(extractDir, "sub/test2.txt")
        assertTrue(extractedFile1.exists())
        assertTrue(extractedFile2.exists())
        assertEquals("Hello Android TV", extractedFile1.readText())
        assertEquals("Nested file content", extractedFile2.readText())
    }
}
