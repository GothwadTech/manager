/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.gothwad.filemanager.misc;

import android.app.Activity;
import com.google.android.material.snackbar.Snackbar;
import android.view.View;

import com.gothwad.filemanager.R;

public final class Snackbars {
    private Snackbars() {}

    public static final Snackbar makeSnackbar(Activity activity, int messageId, int duration) {
        return Snackbars.makeSnackbar(
                activity, activity.getResources().getText(messageId), duration);
    }

    public static final Snackbar makeSnackbar(
            Activity activity, CharSequence message, int duration) {
        if (activity == null || activity.isFinishing()) {
            return null;
        }
        View view = activity.findViewById(R.id.content_view);
        if (view == null) {
            view = activity.findViewById(android.R.id.content);
        }
        if (view == null && activity.getWindow() != null) {
            view = activity.getWindow().getDecorView();
        }
        if (view == null) {
            return null;
        }
        try {
            return Snackbar.make(view, message, duration);
        } catch (Throwable e) {
            e.printStackTrace();
            return null;
        }
    }
}
