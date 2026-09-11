package com.gothwad.manager

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

class FileSortOptionTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    @Test
    fun directoriesAlwaysStayOnTopRegardlessOfName() {
        val root = tempFolder.newFolder("sort_dir")
        val fileA = File(root, "a_file.txt").apply { createNewFile() }
        val dirZ = File(root, "z_folder").apply { mkdir() }

        val sorted = FileSortOption.sort(listOf(fileA, dirZ), FileSortOption.NAME_ASC)

        assertEquals(2, sorted.size)
        assertTrue("Directory must come first", sorted[0].isDirectory)
        assertEquals("z_folder", sorted[0].name)
        assertEquals("a_file.txt", sorted[1].name)
    }

    @Test
    fun sortsByNameAscendingAndDescending() {
        val root = tempFolder.newFolder("names")
        val fileB = File(root, "banana.txt").apply { createNewFile() }
        val fileA = File(root, "apple.txt").apply { createNewFile() }
        val fileC = File(root, "cherry.txt").apply { createNewFile() }

        val asc = FileSortOption.sort(listOf(fileB, fileA, fileC), FileSortOption.NAME_ASC)
        assertEquals("apple.txt", asc[0].name)
        assertEquals("banana.txt", asc[1].name)
        assertEquals("cherry.txt", asc[2].name)

        val desc = FileSortOption.sort(listOf(fileB, fileA, fileC), FileSortOption.NAME_DESC)
        assertEquals("cherry.txt", desc[0].name)
        assertEquals("banana.txt", desc[1].name)
        assertEquals("apple.txt", desc[2].name)
    }

    @Test
    fun sortsBySizeAscendingAndDescending() {
        val root = tempFolder.newFolder("sizes")
        val small = File(root, "small.bin").apply {
            createNewFile()
            writeBytes(ByteArray(10))
        }
        val large = File(root, "large.bin").apply {
            createNewFile()
            writeBytes(ByteArray(500))
        }

        val desc = FileSortOption.sort(listOf(small, large), FileSortOption.SIZE_DESC)
        assertEquals("large.bin", desc[0].name)
        assertEquals("small.bin", desc[1].name)

        val asc = FileSortOption.sort(listOf(small, large), FileSortOption.SIZE_ASC)
        assertEquals("small.bin", asc[0].name)
        assertEquals("large.bin", asc[1].name)
    }
}
