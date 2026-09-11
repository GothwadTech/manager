package com.example.tvfilebox;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import java.io.File;
import java.text.DateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

final class FileEntryAdapter extends BaseAdapter {
    private final Context context;
    private final LayoutInflater inflater;
    private List<File> files;

    FileEntryAdapter(Context context, List<File> files) {
        this.context = context;
        this.inflater = LayoutInflater.from(context);
        this.files = files;
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
            convertView = inflater.inflate(R.layout.row_file, parent, false);
            holder = new Holder();
            holder.icon = (TextView) convertView.findViewById(R.id.file_icon);
            holder.name = (TextView) convertView.findViewById(R.id.file_name);
            holder.meta = (TextView) convertView.findViewById(R.id.file_meta);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        final File file = getItem(position);
        String lower = file.getName().toLowerCase(Locale.US);
        holder.icon.setText(file.isDirectory() ? "[D]" : lower.endsWith(".apk") ? "APK" : "[F]");
        holder.name.setText(file.getName());
        String modified = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
                .format(new Date(file.lastModified()));
        holder.meta.setText(file.isDirectory() ? context.getString(R.string.folder) + "  ·  " + modified
                : FileUtils.formatSize(file.length()) + "  ·  " + modified);
        return convertView;
    }

    private static class Holder {
        TextView icon;
        TextView name;
        TextView meta;
    }
}
