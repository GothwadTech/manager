package com.example.tvfilebox;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.content.Context;
import android.widget.Button;
import android.widget.EditText;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private static final int STORAGE_PERMISSION_REQUEST = 100;

    private TextView pathView;
    private ListView listView;
    private FileEntryAdapter adapter;
    private File initialDirectory;
    private File currentDirectory;
    private File clipboardFile;
    private boolean clipboardCut;
    private View selectedRowView;
    private boolean confirmLongPressHandled;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        pathView = (TextView) findViewById(R.id.text_path);
        listView = (ListView) findViewById(R.id.file_list);
        initialDirectory = Environment.getExternalStorageDirectory();
        currentDirectory = initialDirectory;
        adapter = new FileEntryAdapter(this, FileUtils.listFiles(currentDirectory));
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openEntry(adapter.getItem(position));
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmLongPressHandled = true;
                deleteEntry(adapter.getItem(position));
                return true;
            }
        });
        listView.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (selectedRowView != null && selectedRowView != view) {
                    selectedRowView.setSelected(false);
                }
                selectedRowView = view;
                // AbsListView keeps a selected position even while a toolbar
                // button has the real focus. Only draw the row highlight while
                // the file list itself owns focus.
                view.setSelected(listView.hasFocus());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                if (selectedRowView != null) selectedRowView.setSelected(false);
                selectedRowView = null;
            }
        });
        listView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
            @Override
            public void onFocusChange(View v, boolean hasFocus) {
                if (hasFocus) {
                    if (listView.getSelectedItemPosition() == AdapterView.INVALID_POSITION
                            && adapter.getCount() > 0) {
                        listView.setSelection(0);
                    } else if (selectedRowView != null) {
                        selectedRowView.setSelected(true);
                    }
                } else if (selectedRowView != null) {
                    selectedRowView.setSelected(false);
                }
            }
        });

        ((Button) findViewById(R.id.button_home)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openDirectory(initialDirectory); }
        });
        ((Button) findViewById(R.id.button_new_folder)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { showCreateFolderDialog(); }
        });
        ((Button) findViewById(R.id.button_transfer)).setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { openTransfer(); }
        });

        requestStoragePermissionIfNeeded();
        refresh();
        findViewById(R.id.button_transfer).requestFocus();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (adapter != null) refresh();
    }

    private void requestStoragePermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= 23 &&
                checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            requestPermissions(new String[] {
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
            }, STORAGE_PERMISSION_REQUEST);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == STORAGE_PERMISSION_REQUEST) {
            refresh();
            if (grantResults.length == 0 || grantResults[0] != PackageManager.PERMISSION_GRANTED) {
                Toast.makeText(this, R.string.storage_permission_denied, Toast.LENGTH_LONG).show();
                openDirectory(FileUtils.uploadDirectory(this));
            }
        }
    }

    private void refresh() {
        if (currentDirectory == null || !currentDirectory.exists() || !currentDirectory.canRead()) {
            currentDirectory = FileUtils.uploadDirectory(this);
        }
        pathView.setText(getString(R.string.current_path, currentDirectory.getAbsolutePath()));
        adapter.replace(FileUtils.listFiles(currentDirectory));
    }

    private void openDirectory(File directory) {
        if (directory != null && directory.isDirectory() && directory.canRead()) {
            currentDirectory = directory;
            refresh();
            listView.setSelection(0);
        } else {
            Toast.makeText(this, R.string.cannot_access_directory, Toast.LENGTH_SHORT).show();
        }
    }

    private void navigateParent() {
        File parent = currentDirectory == null ? null : currentDirectory.getParentFile();
        if (parent != null && parent.canRead()) openDirectory(parent);
        else Toast.makeText(this, R.string.top_directory_reached, Toast.LENGTH_SHORT).show();
    }

    private void showCreateFolderDialog() {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setHint(R.string.folder_name_hint);
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.new_folder)
                .setView(input)
                .setPositiveButton(R.string.create, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (!isSimpleName(name)) {
                            Toast.makeText(MainActivity.this, R.string.invalid_simple_name, Toast.LENGTH_LONG).show();
                            return;
                        }
                        File folder = new File(currentDirectory, name);
                        if (folder.exists()) {
                            Toast.makeText(MainActivity.this, R.string.file_or_folder_exists, Toast.LENGTH_SHORT).show();
                        } else if (!folder.mkdir()) {
                            Toast.makeText(MainActivity.this, R.string.create_folder_failed, Toast.LENGTH_LONG).show();
                        }
                        refresh();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialog) {
                input.requestFocus();
                dialogKeyboard(input);
            }
        });
        dialog.show();
    }

    private void openEntry(File file) {
        if (file.isDirectory()) openDirectory(file);
        else FileUtils.openFile(this, file);
    }

    private void renameEntry(final File file) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setText(file.getName());
        input.setSelection(input.getText().length());
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.rename)
                .setView(input)
                .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        String name = input.getText().toString().trim();
                        if (!isSimpleName(name)) {
                            Toast.makeText(MainActivity.this, R.string.invalid_name, Toast.LENGTH_SHORT).show();
                            return;
                        }
                        File destination = new File(file.getParentFile(), name);
                        if (destination.exists()) {
                            Toast.makeText(MainActivity.this, R.string.file_exists, Toast.LENGTH_SHORT).show();
                        } else if (!file.renameTo(destination)) {
                            Toast.makeText(MainActivity.this, R.string.rename_failed, Toast.LENGTH_SHORT).show();
                        } else if (FileUtils.sameFile(clipboardFile, file)) {
                            clipboardFile = destination;
                        }
                        refresh();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .create();
        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override public void onShow(DialogInterface dialog) { input.requestFocus(); dialogKeyboard(input); }
        });
        dialog.show();
    }

    private void deleteEntry(final File file) {
        new AlertDialog.Builder(this)
                .setTitle(R.string.permanent_delete)
                .setMessage(getString(R.string.delete_confirm, file.getName()))
                .setPositiveButton(R.string.permanent_delete, new DialogInterface.OnClickListener() {
                    @Override public void onClick(DialogInterface dialog, int which) {
                        if (!FileUtils.deleteRecursively(file)) {
                            Toast.makeText(MainActivity.this, R.string.delete_failed_detailed, Toast.LENGTH_LONG).show();
                        } else if (FileUtils.sameFile(clipboardFile, file)) {
                            clearClipboard();
                        }
                        refresh();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private boolean isSimpleName(String name) {
        return name.length() > 0 && !".".equals(name) && !"..".equals(name)
                && name.indexOf('/') < 0 && name.indexOf('\\') < 0;
    }

    private void dialogKeyboard(EditText input) {
        InputMethodManager keyboard = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        if (keyboard != null) keyboard.showSoftInput(input, InputMethodManager.SHOW_IMPLICIT);
    }

    private void openTransfer() {
        startActivity(new Intent(this, TransferActivity.class));
    }

    private void putOnClipboard(File file, boolean cut) {
        clipboardFile = file;
        clipboardCut = cut;
        Toast.makeText(this, getString(cut ? R.string.cut_to_clipboard : R.string.copied_to_clipboard,
                file.getName()), Toast.LENGTH_SHORT).show();
    }

    private void clearClipboard() {
        clipboardFile = null;
        clipboardCut = false;
    }

    private void pasteClipboard(File selected) {
        if (clipboardFile == null || !clipboardFile.exists()) {
            clearClipboard();
            Toast.makeText(this, R.string.clipboard_source_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        File targetDirectory = FileUtils.pasteTargetForSelection(selected);
        if (clipboardCut && FileUtils.sameFile(clipboardFile.getParentFile(), targetDirectory)) {
            Toast.makeText(this, R.string.cut_same_folder, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            if (clipboardFile.isDirectory()
                    && FileUtils.isSameOrDescendant(clipboardFile, targetDirectory)) {
                Toast.makeText(this, R.string.cannot_paste_into_itself, Toast.LENGTH_LONG).show();
                return;
            }
            FileUtils.paste(clipboardFile, targetDirectory, clipboardCut);
            if (clipboardCut) clearClipboard();
            Toast.makeText(this, getString(R.string.paste_success,
                    targetDirectory.getAbsolutePath()), Toast.LENGTH_SHORT).show();
            refresh();
        } catch (IOException e) {
            Toast.makeText(this, R.string.paste_failed, Toast.LENGTH_LONG).show();
        }
    }

    private void showSelectedEntryActions() {
        int position = listView.getSelectedItemPosition();
        if (position == AdapterView.INVALID_POSITION || position >= adapter.getCount()) {
            Toast.makeText(this, R.string.select_file_first, Toast.LENGTH_SHORT).show();
            listView.requestFocus();
            if (adapter.getCount() > 0) listView.setSelection(0);
            return;
        }
        final File selected = adapter.getItem(position);
        if (clipboardFile != null && !clipboardFile.exists()) clearClipboard();
        final List<Integer> actions = new ArrayList<Integer>();
        actions.add(R.string.copy);
        actions.add(R.string.cut);
        if (clipboardFile != null) actions.add(R.string.paste);
        actions.add(R.string.rename);
        actions.add(R.string.delete);
        CharSequence[] labels = new CharSequence[actions.size()];
        for (int i = 0; i < actions.size(); i++) labels[i] = getString(actions.get(i));
        new AlertDialog.Builder(this)
                .setTitle(selected.getName())
                .setItems(labels, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        int action = actions.get(which);
                        if (action == R.string.copy) putOnClipboard(selected, false);
                        else if (action == R.string.cut) putOnClipboard(selected, true);
                        else if (action == R.string.paste) pasteClipboard(selected);
                        else if (action == R.string.rename) renameEntry(selected);
                        else deleteEntry(selected);
                    }
                })
                .show();
    }

    private boolean deleteSelectedEntryFromRemote() {
        int position = listView.getSelectedItemPosition();
        if (!listView.hasFocus() || position == AdapterView.INVALID_POSITION || position >= adapter.getCount()) {
            return false;
        }
        deleteEntry(adapter.getItem(position));
        return true;
    }

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int keyCode = event.getKeyCode();
        boolean contextMenuKey = RemoteKeyPolicy.isContextMenuKey(keyCode);
        if (contextMenuKey) {
            if (RemoteKeyPolicy.shouldOpenEntryActions(event.getAction(), keyCode, event.getRepeatCount())) {
                showSelectedEntryActions();
            }
            // Consume both DOWN and UP so the system does not handle the key again on release.
            return true;
        }
        if (RemoteKeyPolicy.isConfirmKey(keyCode)) {
            if (event.getAction() == KeyEvent.ACTION_DOWN) {
                if (event.getRepeatCount() == 0) {
                    confirmLongPressHandled = false;
                } else if (RemoteKeyPolicy.shouldHandleConfirmLongPress(
                        event.getAction(), keyCode, event.getRepeatCount())) {
                    if (!confirmLongPressHandled) {
                        confirmLongPressHandled = deleteSelectedEntryFromRemote();
                    }
                    if (confirmLongPressHandled) return true;
                }
            } else if (event.getAction() == KeyEvent.ACTION_UP && confirmLongPressHandled) {
                confirmLongPressHandled = false;
                return true;
            }
        }
        return super.dispatchKeyEvent(event);
    }

    @Override
    public void onBackPressed() {
        File external = Environment.getExternalStorageDirectory();
        if (currentDirectory != null && external != null &&
                !currentDirectory.getAbsolutePath().equals(external.getAbsolutePath()) &&
                currentDirectory.getParentFile() != null && currentDirectory.getParentFile().canRead()) {
            navigateParent();
        } else {
            super.onBackPressed();
        }
    }
}
