package com.example.tvfilebox;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

public class LocalFileProvider extends ContentProvider {
    private static final String PARAM_PATH = "path";

    static Uri uriForFile(Context context, File file) {
        return new Uri.Builder()
                .scheme("content")
                .authority(context.getPackageName() + ".files")
                .appendPath("file")
                .appendQueryParameter(PARAM_PATH, file.getAbsolutePath())
                .build();
    }

    @Override
    public boolean onCreate() {
        return true;
    }

    private File fileFromUri(Uri uri) throws FileNotFoundException {
        String path = uri.getQueryParameter(PARAM_PATH);
        if (path == null) throw new FileNotFoundException("Missing path");
        File file = new File(path);
        if (!file.exists() || !file.isFile() || !file.canRead()) {
            throw new FileNotFoundException(path);
        }
        return file;
    }

    @Override
    public String getType(Uri uri) {
        try {
            return FileUtils.mimeType(fileFromUri(uri));
        } catch (FileNotFoundException e) {
            return "application/octet-stream";
        }
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        if (!"r".equals(mode)) throw new FileNotFoundException("Read only");
        return ParcelFileDescriptor.open(fileFromUri(uri), ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        try {
            File file = fileFromUri(uri);
            MatrixCursor cursor = new MatrixCursor(new String[] {
                    OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE
            });
            cursor.addRow(new Object[] { file.getName(), file.length() });
            return cursor;
        } catch (FileNotFoundException e) {
            return null;
        }
    }

    @Override public Uri insert(Uri uri, ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri, String selection, String[] selectionArgs) { return 0; }
    @Override public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) { return 0; }
}
