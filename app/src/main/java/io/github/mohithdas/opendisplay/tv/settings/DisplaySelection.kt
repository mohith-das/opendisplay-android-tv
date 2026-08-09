package io.github.mohithdas.opendisplay.tv.settings

import kotlin.math.min
import kotlin.math.roundToInt

data class PixelSize(val width: Int, val height: Int) {
    init {
        require(width > 0 && height > 0)
    }

    val longEdge: Int get() = maxOf(width, height)
    val shortEdge: Int get() = minOf(width, height)
}

data class DisplayProfile(
    val physicalMode: PixelSize,
    val windowBounds: PixelSize,
    val decoderMaximum: PixelSize,
    val isTelevision: Boolean,
    val androidDensity: Double,
)

data class DisplaySelection(val pixels: PixelSize, val scale: Double)

object DisplaySelector {
    private val FULL_HD = PixelSize(1920, 1080)
    private val HD = PixelSize(1280, 720)

    fun supportedChoices(profile: DisplayProfile): List<ResolutionChoice> = buildList {
        add(ResolutionChoice.AUTO)
        if (supports(FULL_HD, profile)) add(ResolutionChoice.FULL_HD)
        if (supports(HD, profile)) add(ResolutionChoice.HD)
    }

    fun select(
        profile: DisplayProfile,
        resolution: ResolutionChoice,
        uiScale: UiScaleChoice,
    ): DisplaySelection {
        val requested = when (resolution) {
            ResolutionChoice.AUTO -> automaticSize(profile)
            ResolutionChoice.FULL_HD -> oriented(FULL_HD, profile.windowBounds)
            ResolutionChoice.HD -> oriented(HD, profile.windowBounds)
        }
        val safe = fitWithin(requested, profile.physicalMode, profile.decoderMaximum)
        val scale = when (uiScale) {
            UiScaleChoice.AUTO -> if (profile.isTelevision || profile.androidDensity < 1.5) 1.0 else 2.0
            UiScaleChoice.ONE -> 1.0
            UiScaleChoice.TWO -> 2.0
        }
        return DisplaySelection(safe.even(), scale)
    }

    private fun automaticSize(profile: DisplayProfile): PixelSize {
        val native = if (profile.isTelevision) profile.physicalMode else profile.windowBounds
        // 4K is deliberately opt-out for this first TV release: inexpensive sticks commonly
        // expose a 4K HDMI mode while only sustaining low-latency AVC reliably at 1080p.
        return if (profile.isTelevision && native.longEdge > FULL_HD.longEdge) {
            oriented(FULL_HD, native)
        } else {
            native
        }
    }

    private fun supports(candidate: PixelSize, profile: DisplayProfile): Boolean =
        candidate.longEdge <= profile.physicalMode.longEdge &&
            candidate.shortEdge <= profile.physicalMode.shortEdge &&
            candidate.longEdge <= profile.decoderMaximum.longEdge &&
            candidate.shortEdge <= profile.decoderMaximum.shortEdge

    private fun oriented(size: PixelSize, orientation: PixelSize): PixelSize =
        if ((orientation.width >= orientation.height) == (size.width >= size.height)) size
        else PixelSize(size.height, size.width)

    private fun fitWithin(value: PixelSize, vararg limits: PixelSize): PixelSize {
        var scale = 1.0
        limits.forEach { limit ->
            val direct = min(
                limit.width.toDouble() / value.width,
                limit.height.toDouble() / value.height,
            )
            scale = min(scale, direct)
        }
        return if (scale >= 1.0) value else PixelSize(
            (value.width * scale).roundToInt().coerceAtLeast(2),
            (value.height * scale).roundToInt().coerceAtLeast(2),
        )
    }

    private fun PixelSize.even(): PixelSize = PixelSize(width and -2, height and -2)
}
