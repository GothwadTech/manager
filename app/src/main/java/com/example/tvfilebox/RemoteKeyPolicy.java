package com.example.tvfilebox;

import android.view.KeyEvent;

/** Centralizes TV remote key handling so the mapping can be unit tested. */
final class RemoteKeyPolicy {
    private RemoteKeyPolicy() {}

    static boolean isContextMenuKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU;
    }

    static boolean shouldOpenEntryActions(int action, int keyCode, int repeatCount) {
        return isContextMenuKey(keyCode)
                && action == KeyEvent.ACTION_DOWN
                && repeatCount == 0;
    }

    static boolean isConfirmKey(int keyCode) {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER
                || keyCode == KeyEvent.KEYCODE_ENTER
                || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER;
    }

    static boolean shouldHandleConfirmLongPress(int action, int keyCode, int repeatCount) {
        return isConfirmKey(keyCode)
                && action == KeyEvent.ACTION_DOWN
                && repeatCount > 0;
    }

    static int nextUploadPosition(int currentPosition, int keyCode, int itemCount) {
        int target;
        if (keyCode == KeyEvent.KEYCODE_DPAD_DOWN) {
            target = currentPosition + 1;
        } else if (keyCode == KeyEvent.KEYCODE_DPAD_UP) {
            target = currentPosition - 1;
        } else {
            return -1;
        }
        return target >= 0 && target < itemCount ? target : -1;
    }
}
