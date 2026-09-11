package com.gothwad.manager

import android.content.Context
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.TextView
import java.io.File
import java.text.DateFormat
import java.util.Date
import java.util.Locale

internal class FileEntryAdapter(
    private val context: Context,
    private var files: List<File>
) : BaseAdapter() {

    private val inflater: LayoutInflater = LayoutInflater.from(context)
    private val density: Float = context.resources.displayMetrics.density

    var isMultiSelectMode: Boolean = false
        private set

    val selectedFiles: MutableSet<File> = LinkedHashSet()

    fun setMultiSelectMode(enabled: Boolean) {
        isMultiSelectMode = enabled
        if (!enabled) {
            selectedFiles.clear()
        }
        notifyDataSetChanged()
    }

    fun toggleSelection(file: File): Boolean {
        val isNowSelected = if (selectedFiles.contains(file)) {
            selectedFiles.remove(file)
            false
        } else {
            selectedFiles.add(file)
            true
        }
        notifyDataSetChanged()
        return isNowSelected
    }

    fun selectAll() {
        selectedFiles.clear()
        selectedFiles.addAll(files)
        notifyDataSetChanged()
    }

    fun clearSelection() {
        selectedFiles.clear()
        notifyDataSetChanged()
    }

    fun replace(updated: List<File>) {
        files = updated
        // Filter out selected files that no longer exist in the updated list
        selectedFiles.retainAll(files.toSet())
        notifyDataSetChanged()
    }

    override fun getCount(): Int = files.size
    override fun getItem(position: Int): File = files[position]
    override fun getItemId(position: Int): Long = position.toLong()

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: Holder
        if (convertView == null) {
            view = inflater.inflate(R.layout.row_file, parent, false)
            holder = Holder(
                checkbox = view.findViewById(R.id.file_checkbox),
                icon = view.findViewById(R.id.file_icon),
                name = view.findViewById(R.id.file_name),
                meta = view.findViewById(R.id.file_meta)
            )
            view.tag = holder
        } else {
            view = convertView
            holder = view.tag as Holder
        }

        val file = getItem(position)
        bindBadge(holder.icon, file)
        holder.name.text = file.name

        if (isMultiSelectMode) {
            holder.checkbox.visibility = View.VISIBLE
            val isSelected = selectedFiles.contains(file)
            if (isSelected) {
                holder.checkbox.setBackgroundResource(R.drawable.checkbox_checked)
                holder.checkbox.text = "✓"
            } else {
                holder.checkbox.setBackgroundResource(R.drawable.checkbox_unchecked)
                holder.checkbox.text = ""
            }
        } else {
            holder.checkbox.visibility = View.GONE
        }

        val modified = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT)
            .format(Date(file.lastModified()))
        if (file.isDirectory) {
            val subs = file.listFiles()
            val subCount = subs?.size ?: 0
            holder.meta.text = "${context.getString(R.string.folder)}  ·  $subCount items  ·  $modified"
        } else {
            holder.meta.text = "${FileUtils.formatSize(file.length())}  ·  $modified"
        }
        return view
    }

    private fun bindBadge(iconView: TextView, file: File) {
        val label: String
        val bgColor: Int
        val textColor: Int

        if (file.isDirectory) {
            label = "DIR"
            bgColor = 0xFF2D1F07.toInt()
            textColor = 0xFFFBBF24.toInt()
        } else {
            val name = file.name.lowercase(Locale.US)
            when {
                name.endsWith(".apk") -> {
                    label = "APK"
                    bgColor = 0xFF042F1A.toInt()
                    textColor = 0xFF34D399.toInt()
                }
                name.endsWith(".mp4") || name.endsWith(".mkv") || name.endsWith(".avi") ||
                        name.endsWith(".mov") || name.endsWith(".webm") || name.endsWith(".ts") -> {
                    label = "VIDEO"
                    bgColor = 0xFF2E1065.toInt()
                    textColor = 0xFFC084FC.toInt()
                }
                name.endsWith(".mp3") || name.endsWith(".m4a") || name.endsWith(".flac") ||
                        name.endsWith(".wav") || name.endsWith(".ogg") || name.endsWith(".aac") -> {
                    label = "AUDIO"
                    bgColor = 0xFF3D0C26.toInt()
                    textColor = 0xFFF472B6.toInt()
                }
                name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") ||
                        name.endsWith(".webp") || name.endsWith(".gif") || name.endsWith(".bmp") -> {
                    label = "IMAGE"
                    bgColor = 0xFF0C2D48.toInt()
                    textColor = 0xFF38BDF8.toInt()
                }
                name.endsWith(".zip") || name.endsWith(".rar") || name.endsWith(".7z") ||
                        name.endsWith(".tar") || name.endsWith(".gz") -> {
                    label = "ZIP"
                    bgColor = 0xFF341A04.toInt()
                    textColor = 0xFFFB923C.toInt()
                }
                name.endsWith(".pdf") || name.endsWith(".txt") || name.endsWith(".doc") ||
                        name.endsWith(".docx") || name.endsWith(".log") || name.endsWith(".json") -> {
                    label = "DOC"
                    bgColor = 0xFF1E1B4B.toInt()
                    textColor = 0xFF818CF8.toInt()
                }
                else -> {
                    label = "FILE"
                    bgColor = 0xFF1E293B.toInt()
                    textColor = 0xFF94A3B8.toInt()
                }
            }
        }

        iconView.text = label
        iconView.setTextColor(textColor)
        val gd = GradientDrawable().apply {
            setColor(bgColor)
            cornerRadius = 6 * density
            setStroke((1 * density).toInt(), textColor and 0x55FFFFFF)
        }
        iconView.background = gd
    }

    private class Holder(
        val checkbox: TextView,
        val icon: TextView,
        val name: TextView,
        val meta: TextView
    )
}
