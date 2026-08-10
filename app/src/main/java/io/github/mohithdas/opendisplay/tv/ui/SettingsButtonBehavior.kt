package io.github.mohithdas.opendisplay.tv.ui

import android.view.KeyEvent

internal object SettingsButtonBehavior {
    const val AUTO_HIDE_DELAY_MILLIS = 5_000L

    fun shouldAutoHide(connected: Boolean, settingsOpen: Boolean): Boolean =
        connected && !settingsOpen

    fun opensSettings(keyCode: Int): Boolean = keyCode == KeyEvent.KEYCODE_MENU ||
        keyCode == KeyEvent.KEYCODE_SETTINGS

    fun isRemoteInteraction(keyCode: Int): Boolean = keyCode != KeyEvent.KEYCODE_UNKNOWN

    fun focusesRevealedButton(keyCode: Int): Boolean = when (keyCode) {
        KeyEvent.KEYCODE_DPAD_UP,
        KeyEvent.KEYCODE_DPAD_DOWN,
        KeyEvent.KEYCODE_DPAD_LEFT,
        KeyEvent.KEYCODE_DPAD_RIGHT,
        KeyEvent.KEYCODE_DPAD_CENTER,
        KeyEvent.KEYCODE_ENTER,
        KeyEvent.KEYCODE_NUMPAD_ENTER -> true
        else -> false
    }
}
