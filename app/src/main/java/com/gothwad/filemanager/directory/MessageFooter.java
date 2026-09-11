package com.gothwad.manager.directory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.gothwad.manager.BaseActivity;
import com.gothwad.manager.R;
import com.gothwad.manager.directory.DocumentsAdapter.Environment;

import static com.gothwad.manager.BaseActivity.State.MODE_GRID;
import static com.gothwad.manager.BaseActivity.State.MODE_LIST;

public class MessageFooter extends Footer {

    public MessageFooter(Environment environment, int itemViewType, int icon, String message) {
        super(itemViewType);
        mIcon = icon;
        mMessage = message;
        mEnv = environment;
    }
}