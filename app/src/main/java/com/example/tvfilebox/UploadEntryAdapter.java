package com.example.tvfilebox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class UploadEntryAdapter extends BaseAdapter {
    interface Listener {
        void onOpen(File file);
        void onDelete(File file);
    }

    private final LayoutInflater inflater;
    private final Listener listener;
    private List<File> files;

    UploadEntryAdapter(Context context, List<File> files, Listener listener) {
        inflater = LayoutInflater.from(context);
        this.files = files;
        this.listener = listener;
    }

    void replace(List<File> updated) {
        files = updated;
        notifyDataSetChanged();
    }

    @Override public int getCount() { return files.size(); }
    @Override public File getItem(int position) { return files.get(position); }
    @Override public long getItemId(int position) { return position; }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        Holder holder;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.row_upload, parent, false);
            holder = new Holder();
            holder.name = (TextView) convertView.findViewById(R.id.upload_name);
            holder.meta = (TextView) convertView.findViewById(R.id.upload_meta);
            holder.open = (Button) convertView.findViewById(R.id.upload_open);
            holder.delete = (Button) convertView.findViewById(R.id.upload_delete);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }
        final File file = getItem(position);
        boolean apk = file.getName().toLowerCase(Locale.US).endsWith(".apk");
        holder.name.setText(file.getName());
        holder.meta.setText(FileUtils.formatSize(file.length()) + "  ·  " +
                DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                        .format(new Date(file.lastModified())));
        holder.open.setText(apk ? R.string.install : R.string.open);
        holder.open.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { listener.onOpen(file); }
        });
        holder.delete.setOnClickListener(new View.OnClickListener() {
            @Override public void onClick(View v) { listener.onDelete(file); }
        });
        return convertView;
    }

    private static class Holder {
        TextView name;
        TextView meta;
        Button open;
        Button delete;
    }
}
