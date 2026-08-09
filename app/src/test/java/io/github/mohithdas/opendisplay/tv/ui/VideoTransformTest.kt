package io.github.mohithdas.opendisplay.tv.ui

import io.github.mohithdas.opendisplay.tv.settings.FitMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VideoTransformTest {
    @Test
    fun fitLetterboxesAndMapsCoordinates() {
        val rect = VideoTransform.destination(1920, 1200, 1920, 1080, FitMode.FIT)
        assertEquals(VideoRect(0.0, 60.0, 1920.0, 1080.0), rect)
        assertNull(VideoTransform.mapToVideo(100.0, 20.0, rect, rejectOutside = true))
        val center = VideoTransform.mapToVideo(960.0, 600.0, rect, rejectOutside = true)!!
        assertEquals(0.5, center.x, 0.0001)
        assertEquals(0.5, center.y, 0.0001)
    }

    @Test
    fun fillCropsSymmetricallyAndMapsVisibleEdges() {
        val rect = VideoTransform.destination(1920, 1200, 1920, 1080, FitMode.FILL)
        assertEquals(0.0, rect.top, 0.0001)
        assertEquals(1200.0, rect.height, 0.0001)
        val left = VideoTransform.mapToVideo(0.0, 600.0, rect, rejectOutside = false)!!
        assertEquals(0.05, left.x, 0.001)
        assertEquals(0.5, left.y, 0.001)
    }

    @Test
    fun stretchUsesWholeContainer() {
        assertEquals(
            VideoRect(0.0, 0.0, 1000.0, 1000.0),
            VideoTransform.destination(1000, 1000, 1920, 1080, FitMode.STRETCH),
        )
    }

    @Test
    fun nativeModeKeepsOneToOnePixels() {
        assertEquals(
            VideoRect(-460.0, -40.0, 1920.0, 1080.0),
            VideoTransform.destination(1000, 1000, 1920, 1080, FitMode.NATIVE),
        )
    }
}
