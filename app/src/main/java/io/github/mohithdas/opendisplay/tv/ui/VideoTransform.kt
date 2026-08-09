package io.github.mohithdas.opendisplay.tv.ui

import io.github.mohithdas.opendisplay.tv.settings.FitMode
import kotlin.math.max
import kotlin.math.min

data class VideoRect(val left: Double, val top: Double, val width: Double, val height: Double)

data class NormalizedPoint(val x: Double, val y: Double)

object VideoTransform {
    fun destination(
        containerWidth: Int,
        containerHeight: Int,
        videoWidth: Int,
        videoHeight: Int,
        mode: FitMode,
    ): VideoRect {
        require(containerWidth > 0 && containerHeight > 0 && videoWidth > 0 && videoHeight > 0)
        val widthScale = containerWidth.toDouble() / videoWidth
        val heightScale = containerHeight.toDouble() / videoHeight
        val scale = when (mode) {
            FitMode.FIT -> min(widthScale, heightScale)
            FitMode.FILL -> max(widthScale, heightScale)
            FitMode.NATIVE -> 1.0
            FitMode.STRETCH -> return VideoRect(0.0, 0.0, containerWidth.toDouble(), containerHeight.toDouble())
        }
        val width = videoWidth * scale
        val height = videoHeight * scale
        return VideoRect(
            left = (containerWidth - width) / 2.0,
            top = (containerHeight - height) / 2.0,
            width = width,
            height = height,
        )
    }

    fun mapToVideo(x: Double, y: Double, rect: VideoRect, rejectOutside: Boolean): NormalizedPoint? {
        val nx = (x - rect.left) / rect.width
        val ny = (y - rect.top) / rect.height
        if (rejectOutside && (nx !in 0.0..1.0 || ny !in 0.0..1.0)) return null
        return NormalizedPoint(nx.coerceIn(0.0, 1.0), ny.coerceIn(0.0, 1.0))
    }
}

