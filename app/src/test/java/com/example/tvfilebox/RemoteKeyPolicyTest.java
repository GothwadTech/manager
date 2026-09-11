package com.example.tvfilebox;

import android.view.KeyEvent;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RemoteKeyPolicyTest {
    @Test
    public void settingsAndMenuKeysAreAccepted() {
        assertTrue(RemoteKeyPolicy.isContextMenuKey(KeyEvent.KEYCODE_SETTINGS));
        assertTrue(RemoteKeyPolicy.isContextMenuKey(KeyEvent.KEYCODE_MENU));
        assertFalse(RemoteKeyPolicy.isContextMenuKey(KeyEvent.KEYCODE_DPAD_CENTER));
    }

    @Test
    public void actionsOpenOnlyOnFirstKeyDown() {
        assertTrue(RemoteKeyPolicy.shouldOpenEntryActions(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SETTINGS, 0));
        assertTrue(RemoteKeyPolicy.shouldOpenEntryActions(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MENU, 0));
        assertFalse(RemoteKeyPolicy.shouldOpenEntryActions(
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_SETTINGS, 0));
        assertFalse(RemoteKeyPolicy.shouldOpenEntryActions(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_SETTINGS, 1));
    }

    @Test
    public void confirmKeysSupportLongPressWithoutChangingShortPress() {
        assertTrue(RemoteKeyPolicy.isConfirmKey(KeyEvent.KEYCODE_DPAD_CENTER));
        assertTrue(RemoteKeyPolicy.isConfirmKey(KeyEvent.KEYCODE_ENTER));
        assertTrue(RemoteKeyPolicy.isConfirmKey(KeyEvent.KEYCODE_NUMPAD_ENTER));
        assertFalse(RemoteKeyPolicy.isConfirmKey(KeyEvent.KEYCODE_DPAD_DOWN));

        assertFalse(RemoteKeyPolicy.shouldHandleConfirmLongPress(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 0));
        assertTrue(RemoteKeyPolicy.shouldHandleConfirmLongPress(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_DPAD_CENTER, 1));
        assertTrue(RemoteKeyPolicy.shouldHandleConfirmLongPress(
                KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_ENTER, 2));
        assertFalse(RemoteKeyPolicy.shouldHandleConfirmLongPress(
                KeyEvent.ACTION_UP, KeyEvent.KEYCODE_DPAD_CENTER, 1));
    }

    @Test
    public void verticalUploadNavigationTargetsAdjacentRowsOnly() {
        assertEquals(1, RemoteKeyPolicy.nextUploadPosition(0, KeyEvent.KEYCODE_DPAD_DOWN, 3));
        assertEquals(2, RemoteKeyPolicy.nextUploadPosition(1, KeyEvent.KEYCODE_DPAD_DOWN, 3));
        assertEquals(0, RemoteKeyPolicy.nextUploadPosition(1, KeyEvent.KEYCODE_DPAD_UP, 3));
        assertEquals(-1, RemoteKeyPolicy.nextUploadPosition(0, KeyEvent.KEYCODE_DPAD_UP, 3));
        assertEquals(-1, RemoteKeyPolicy.nextUploadPosition(2, KeyEvent.KEYCODE_DPAD_DOWN, 3));
        assertEquals(-1, RemoteKeyPolicy.nextUploadPosition(1, KeyEvent.KEYCODE_DPAD_LEFT, 3));
    }
}
