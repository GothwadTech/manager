package com.gothwad.filemanager.ui;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;

public class ProWrapper extends FrameLayout {

    public ProWrapper(Context context) {
        super(context);
        setVisibility(View.GONE);
    }

    public ProWrapper(Context context, AttributeSet attrs) {
        super(context, attrs);
        setVisibility(View.GONE);
    }

    public ProWrapper(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        setVisibility(View.GONE);
    }
}
