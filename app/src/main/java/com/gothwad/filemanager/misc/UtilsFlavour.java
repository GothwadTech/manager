package com.gothwad.filemanager.misc;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.google.android.material.snackbar.Snackbar;
import com.gothwad.filemanager.R;
import com.gothwad.filemanager.common.DialogFragment;
import com.gothwad.filemanager.model.DocumentInfo;
import com.gothwad.filemanager.model.RootInfo;
import com.gothwad.filemanager.setting.SettingsActivity;

public class UtilsFlavour {

    public static void showInfo(Context context, int messageId){

    }

    public static Menu getActionDrawerMenu(Activity activity){
        return null;
    }

    public static View getActionDrawer(Activity activity){
        return null;
    }

    public static void inflateActionMenu(Activity activity,
                                         MenuItem.OnMenuItemClickListener listener,
                                         boolean contextual, RootInfo root, DocumentInfo cwd) {
    }

    public static void showMessage(Activity activity, String message,
                                   int duration, String action, View.OnClickListener listener){
        if (activity == null || activity.isFinishing()) {
            return;
        }
        View view = activity.findViewById(R.id.content_view);
        if (view == null) {
            view = activity.findViewById(android.R.id.content);
        }
        if (view == null && activity.getWindow() != null) {
            view = activity.getWindow().getDecorView();
        }
        if (view == null) {
            return;
        }
        try {
            Snackbar snackbar = Snackbar.make(view, message, duration);
            if (null != listener) {
                snackbar.setAction(action, listener)
                        .setActionTextColor(SettingsActivity.getAccentColor());
            }
            snackbar.show();
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
