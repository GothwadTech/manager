package com.example.tvfilebox;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileUtilsTest {
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Test
    public void newestUploadIsListedFirst() throws Exception {
        File directory = temporaryFolder.newFolder("uploads");
        File older = new File(directory, "older.apk");
        File newest = new File(directory, "newest.apk");
        assertTrue(older.createNewFile());
        assertTrue(newest.createNewFile());
        assertTrue(older.setLastModified(1000L));
        assertTrue(newest.setLastModified(2000L));

        List<File> files = FileUtils.listUploadsForDirectory(directory);

        assertEquals(2, files.size());
        assertEquals("newest.apk", files.get(0).getName());
        assertEquals("older.apk", files.get(1).getName());
    }

    @Test
    public void pasteCopiesFileAndAvoidsDuplicateName() throws Exception {
        File directory = temporaryFolder.newFolder("files");
        File source = new File(directory, "movie.mp4");
        FileOutputStream output = new FileOutputStream(source);
        output.write(new byte[] { 1, 2, 3 });
        output.close();

        File destination = FileUtils.paste(source, directory, false);

        assertEquals("movie (1).mp4", destination.getName());
        assertTrue(source.exists());
        assertTrue(destination.exists());
        assertEquals(3L, destination.length());
    }

    @Test
    public void pasteCopiesDirectoryRecursivelyAndKeepsDottedName() throws Exception {
        File parent = temporaryFolder.newFolder("parent");
        File source = new File(parent, "archive.dir");
        assertTrue(source.mkdir());
        assertTrue(new File(source, "inside.txt").createNewFile());

        File destination = FileUtils.paste(source, parent, false);

        assertEquals("archive.dir (1)", destination.getName());
        assertTrue(new File(destination, "inside.txt").exists());
    }

    @Test
    public void pasteMovesItemToAnotherDirectory() throws Exception {
        File sourceDirectory = temporaryFolder.newFolder("source");
        File targetDirectory = temporaryFolder.newFolder("target");
        File source = new File(sourceDirectory, "move.txt");
        assertTrue(source.createNewFile());

        File destination = FileUtils.paste(source, targetDirectory, true);

        assertFalse(source.exists());
        assertTrue(destination.exists());
        assertEquals(targetDirectory.getCanonicalFile(), destination.getParentFile().getCanonicalFile());
    }

    @Test
    public void pasteTargetUsesSelectedFolderOrSelectedFilesParent() throws Exception {
        File parent = temporaryFolder.newFolder("paste-target");
        File folder = new File(parent, "folder");
        File file = new File(parent, "file.txt");
        assertTrue(folder.mkdir());
        assertTrue(file.createNewFile());

        assertEquals(folder.getCanonicalFile(),
                FileUtils.pasteTargetForSelection(folder).getCanonicalFile());
        assertEquals(parent.getCanonicalFile(),
                FileUtils.pasteTargetForSelection(file).getCanonicalFile());
    }

    @Test(expected = IOException.class)
    public void pasteRejectsFolderDestinationInsideSource() throws Exception {
        File source = temporaryFolder.newFolder("tree");
        File child = new File(source, "child");
        assertTrue(child.mkdir());

        FileUtils.paste(source, child, false);
    }
}
