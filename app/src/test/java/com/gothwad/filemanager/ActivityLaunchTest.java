package com.gothwad.filemanager;

import android.content.Intent;
import android.os.Looper;

import com.gothwad.filemanager.setting.SettingsActivity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.Shadows;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLooper;

@RunWith(RobolectricTestRunner.class)
@Config(application = DocumentsApplication.class)
public class ActivityLaunchTest {

    @Test
    public void testDocumentsActivityLaunch() {
        DocumentsActivity activity = Robolectric.buildActivity(DocumentsActivity.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testStandaloneActivityLaunch() {
        StandaloneActivity activity = Robolectric.buildActivity(StandaloneActivity.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testSettingsActivityLaunch() {
        SettingsActivity activity = Robolectric.buildActivity(SettingsActivity.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testAboutActivityLaunch() {
        AboutActivity activity = Robolectric.buildActivity(AboutActivity.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }

    @Test
    public void testNoteActivityLaunch() {
        NoteActivity activity = Robolectric.buildActivity(NoteActivity.class).setup().get();
        Shadows.shadowOf(Looper.getMainLooper()).idle();
    }
}


