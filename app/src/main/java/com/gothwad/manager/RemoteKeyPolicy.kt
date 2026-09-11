package com.gothwad.manager

import android.view.KeyEvent

/** Centralizes TV remote key handling so the mapping can be unit tested. */
object RemoteKeyPolicy {
    @JvmStatic
    fun isContextMenuKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_SETTINGS || keyCode == KeyEvent.KEYCODE_MENU
    }

    @JvmStatic
    fun shouldOpenEntryActions(action: Int, keyCode: Int, repeatCount: Int): Boolean {
        return isContextMenuKey(keyCode) && action == KeyEvent.ACTION_DOWN && repeatCount == 0
    }

    @JvmStatic
    fun isConfirmKey(keyCode: Int): Boolean {
        return keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
                keyCode == KeyEvent.KEYCODE_ENTER ||
                keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER
    }

    @JvmStatic
    fun shouldHandleConfirmLongPress(action: Int, keyCode: Int, repeatCount: Int): Boolean {
        return isConfirmKey(keyCode) && action == KeyEvent.ACTION_DOWN && repeatCount > 0
    }

    @JvmStatic
    fun nextUploadPosition(currentPosition: Int, keyCode: Int, itemCount: Int): Int {
        val target = when (keyCode) {
            KeyEvent.KEYCODE_DPAD_DOWN -> currentPosition + 1
            KeyEvent.KEYCODE_DPAD_UP -> currentPosition - 1
            else -> return -1
        }
        return if (target in 0 until itemCount) target else -1
    }
}
