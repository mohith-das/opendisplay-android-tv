package io.github.mohithdas.opendisplay.tv.settings

import android.app.Activity
import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.hardware.display.DisplayManager
import android.media.MediaCodecList
import android.media.MediaFormat
import android.os.Build
import android.view.Display
import io.github.mohithdas.opendisplay.tv.util.Log

/**
 * Builds receiver display profiles without assuming every [Context] belongs to a display.
 *
 * A Service/Application context is deliberately restricted to DisplayManager and resource
 * metrics. Window metrics and Context.getDisplay() are visual APIs and are used only by the
 * Activity overload.
 */
object DisplayProfileProvider {
    private val HD = PixelSize(1280, 720)
    private val FULL_HD = PixelSize(1920, 1080)

    /** Safe during Application and foreground-service startup, before any Activity exists. */
    fun detectServiceSafe(context: Context): DisplayProfile {
        val appContext = context.applicationContext
        val metrics = appContext.resources.displayMetrics
        val fallback = if (isTelevision(appContext)) FULL_HD else {
            safePixelSize(metrics.widthPixels, metrics.heightPixels) ?: HD
        }
        val physical = try {
            val manager = appContext.getSystemService(DisplayManager::class.java)
            modeSize(manager?.getDisplay(Display.DEFAULT_DISPLAY)) ?: fallback
        } catch (failure: Exception) {
            Log.warn("physical display detection failed; using ${fallback.width}x${fallback.height}", failure)
            fallback
        }

        return profile(
            context = appContext,
            physical = physical,
            window = physical,
            density = metrics.density.toDouble(),
        )
    }

    /** Accurate visual detection. This API intentionally cannot accept a Service Context. */
    fun detectActivity(activity: Activity): DisplayProfile {
        val fallback = detectServiceSafe(activity.applicationContext)
        return try {
            val display = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                activity.display
            } else {
                @Suppress("DEPRECATION")
                activity.windowManager.defaultDisplay
            }
            val physical = modeSize(display) ?: fallback.physicalMode
            val window = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                val bounds = activity.windowManager.currentWindowMetrics.bounds
                safePixelSize(bounds.width(), bounds.height()) ?: fallback.windowBounds
            } else {
                val metrics = activity.resources.displayMetrics
                safePixelSize(metrics.widthPixels, metrics.heightPixels) ?: fallback.windowBounds
            }
            profile(
                context = activity,
                physical = physical,
                window = window,
                density = activity.resources.displayMetrics.density.toDouble(),
            )
        } catch (failure: Exception) {
            Log.warn(
                "visual display detection failed; using service-safe " +
                    "${fallback.physicalMode.width}x${fallback.physicalMode.height} profile",
                failure,
            )
            fallback
        }
    }

    private fun profile(
        context: Context,
        physical: PixelSize,
        window: PixelSize,
        density: Double,
    ): DisplayProfile = DisplayProfile(
        physicalMode = physical,
        windowBounds = window,
        decoderMaximum = reliableAvcMaximum(physical),
        isTelevision = isTelevision(context),
        androidDensity = density.takeIf { it.isFinite() && it > 0.0 } ?: 1.0,
    )

    private fun modeSize(display: Display?): PixelSize? = try {
        val mode = display?.mode ?: return null
        safePixelSize(mode.physicalWidth, mode.physicalHeight)
    } catch (failure: Exception) {
        Log.warn("vendor display mode query failed", failure)
        null
    }

    private fun isTelevision(context: Context): Boolean = try {
        context.getSystemService(UiModeManager::class.java)?.currentModeType ==
            Configuration.UI_MODE_TYPE_TELEVISION
    } catch (failure: Exception) {
        Log.warn("television mode detection failed; using handheld defaults", failure)
        false
    }

    private fun reliableAvcMaximum(physical: PixelSize): PixelSize {
        val candidates = listOf(physical, FULL_HD, HD)
        return try {
            val codec = MediaCodecList(MediaCodecList.REGULAR_CODECS).codecInfos.firstOrNull {
                !it.isEncoder && it.supportedTypes.any { type ->
                    type.equals(MediaFormat.MIMETYPE_VIDEO_AVC, true)
                }
            } ?: return HD
            val capabilities = codec.getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC)
                .videoCapabilities ?: return HD
            candidates.firstOrNull { candidate ->
                try {
                    capabilities.isSizeSupported(candidate.width, candidate.height)
                } catch (failure: Exception) {
                    Log.warn("vendor AVC capability query failed", failure)
                    false
                }
            } ?: HD
        } catch (failure: Exception) {
            Log.warn("AVC decoder detection failed; using conservative 1280x720", failure)
            HD
        }
    }

    private fun safePixelSize(width: Int, height: Int): PixelSize? =
        if (width >= 2 && height >= 2) PixelSize(width, height) else null
}
