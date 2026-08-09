package io.github.mohithdas.opendisplay.tv.settings

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.WindowManager

object DisplayProfileProvider {
    fun detect(context: Context): DisplayProfile {
        val windowManager = context.getSystemService(WindowManager::class.java)
        val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            context.display
        } else {
            @Suppress("DEPRECATION")
            windowManager?.defaultDisplay
        }
        val mode = display?.mode
        val metrics = context.resources.displayMetrics
        val window = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && windowManager != null) {
            val bounds = windowManager.currentWindowMetrics.bounds
            PixelSize(bounds.width().coerceAtLeast(2), bounds.height().coerceAtLeast(2))
        } else {
            PixelSize(metrics.widthPixels.coerceAtLeast(2), metrics.heightPixels.coerceAtLeast(2))
        }
        val physical = if (mode != null) {
            PixelSize(mode.physicalWidth, mode.physicalHeight)
        } else {
            window
        }
        val uiMode = context.getSystemService(UiModeManager::class.java)
        val isTv = uiMode?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION
        return DisplayProfile(
            physicalMode = physical,
            windowBounds = window,
            decoderMaximum = reliableAvcMaximum(physical),
            isTelevision = isTv,
            androidDensity = metrics.density.toDouble(),
        )
    }

    private fun reliableAvcMaximum(physical: PixelSize): PixelSize {
        val candidates = listOf(physical, PixelSize(3840, 2160), PixelSize(1920, 1080), PixelSize(1280, 720))
        return try {
            val codec = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull {
                !it.isEncoder && it.supportedTypes.any { type -> type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true) }
            } ?: return PixelSize(1280, 720)
            val capabilities = codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).videoCapabilities
                ?: return PixelSize(1280, 720)
            candidates.firstOrNull { candidate -> capabilities.isSizeSupported(candidate.width, candidate.height) }
                ?: PixelSize(1280, 720)
        } catch (_: Exception) {
            PixelSize(1920, 1080)
        }
    }
}
