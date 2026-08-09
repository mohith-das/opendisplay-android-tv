package io.github.mohithdas.opendisplay.tv.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DisplaySelectorTest {
    @Test
    fun androidTvAutoCapsFourKAtFullHdAndUsesOneX() {
        val result = DisplaySelector.select(profile(isTv = true), ResolutionChoice.AUTO, UiScaleChoice.AUTO)
        assertEquals(PixelSize(1920, 1080), result.pixels)
        assertEquals(1.0, result.scale, 0.0)
    }

    @Test
    fun phoneAutoUsesWindowAndSensibleDensity() {
        val profile = profile(isTv = false).copy(
            windowBounds = PixelSize(2400, 1080),
            androidDensity = 2.5,
        )
        val result = DisplaySelector.select(profile, ResolutionChoice.AUTO, UiScaleChoice.AUTO)
        assertEquals(PixelSize(2400, 1080), result.pixels)
        assertEquals(2.0, result.scale, 0.0)
    }

    @Test
    fun requestedHdFollowsPortraitOrientation() {
        val profile = profile(isTv = false).copy(windowBounds = PixelSize(1080, 1920))
        assertEquals(
            PixelSize(720, 1280),
            DisplaySelector.select(profile, ResolutionChoice.HD, UiScaleChoice.ONE).pixels,
        )
    }

    @Test
    fun unsupportedResolutionIsNotOffered() {
        val profile = profile(isTv = true).copy(
            physicalMode = PixelSize(1280, 720),
            decoderMaximum = PixelSize(1280, 720),
        )
        val choices = DisplaySelector.supportedChoices(profile)
        assertTrue(ResolutionChoice.HD in choices)
        assertFalse(ResolutionChoice.FULL_HD in choices)
    }

    private fun profile(isTv: Boolean) = DisplayProfile(
        physicalMode = PixelSize(3840, 2160),
        windowBounds = PixelSize(1920, 1080),
        decoderMaximum = PixelSize(3840, 2160),
        isTelevision = isTv,
        androidDensity = 2.0,
    )
}

