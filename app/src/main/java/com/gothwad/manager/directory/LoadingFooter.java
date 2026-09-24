package com.gothwad.manager.directory;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.gothwad.manager.BaseActivity;
import com.gothwad.manager.R;
import com.gothwad.manager.directory.DocumentsAdapter.Environment;

import static com.gothwad.manager.BaseActivity.State.MODE_GRID;
import static com.gothwad.manager.BaseActivity.State.MODE_LIST;

public class LoadingFooter extends Footer {

    public LoadingFooter(Environment environment, int type) {
        super(type);
        mEnv = environment;
        mIcon = 0;
        mMessage = "";
    }
}