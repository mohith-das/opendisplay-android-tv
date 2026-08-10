package io.github.mohithdas.opendisplay.tv.ui

import android.view.KeyEvent
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsButtonBehaviorTest {
    @Test
    fun buttonOnlyAutoHidesWhileConnectedAndDialogIsClosed() {
        assertFalse(SettingsButtonBehavior.shouldAutoHide(connected = false, settingsOpen = false))
        assertFalse(SettingsButtonBehavior.shouldAutoHide(connected = true, settingsOpen = true))
        assertTrue(SettingsButtonBehavior.shouldAutoHide(connected = true, settingsOpen = false))
    }

    @Test
    fun menuAndSettingsKeysOpenSettingsDirectly() {
        assertTrue(SettingsButtonBehavior.opensSettings(KeyEvent.KEYCODE_MENU))
        assertTrue(SettingsButtonBehavior.opensSettings(KeyEvent.KEYCODE_SETTINGS))
        assertFalse(SettingsButtonBehavior.opensSettings(KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun anyKnownRemoteKeyCountsAsInteraction() {
        assertTrue(SettingsButtonBehavior.isRemoteInteraction(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
        assertTrue(SettingsButtonBehavior.isRemoteInteraction(KeyEvent.KEYCODE_VOLUME_DOWN))
        assertFalse(SettingsButtonBehavior.isRemoteInteraction(KeyEvent.KEYCODE_UNKNOWN))
    }

    @Test
    fun dpadAndConfirmationKeysFocusRevealedButton() {
        listOf(
            KeyEvent.KEYCODE_DPAD_UP,
            KeyEvent.KEYCODE_DPAD_DOWN,
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
        ).forEach { assertTrue(SettingsButtonBehavior.focusesRevealedButton(it)) }
        assertFalse(SettingsButtonBehavior.focusesRevealedButton(KeyEvent.KEYCODE_VOLUME_DOWN))
    }
}
