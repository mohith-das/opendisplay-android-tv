package io.github.mohithdas.opendisplay.tv.ui

import android.app.Activity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.unit.dp
import io.github.mohithdas.opendisplay.tv.R
import io.github.mohithdas.opendisplay.tv.net.ListenerPhase
import io.github.mohithdas.opendisplay.tv.net.ListenerState
import io.github.mohithdas.opendisplay.tv.net.NsdPhase
import io.github.mohithdas.opendisplay.tv.net.NsdState
import io.github.mohithdas.opendisplay.tv.net.PeerSignal
import io.github.mohithdas.opendisplay.tv.net.PhoneReceiver
import io.github.mohithdas.opendisplay.tv.settings.FitMode
import io.github.mohithdas.opendisplay.tv.update.UpdateManager
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

@Composable
fun ReceiverScreen(receiver: PhoneReceiver, updateManager: UpdateManager, activity: Activity) {
    val listener by receiver.listenerState.collectAsState()
    val nsd by receiver.nsdState.collectAsState()
    val connected by receiver.connected.collectAsState()
    val settings by receiver.settings.collectAsState()
    val peerSignal by receiver.peerSignal.collectAsState()
    val selection by receiver.displaySelection.collectAsState()
    var videoDims by remember { mutableStateOf<VideoDims?>(null) }
    var showSettings by remember { mutableStateOf(false) }
    var settingsButtonVisible by remember { mutableStateOf(true) }
    var settingsButtonFocusPending by remember { mutableStateOf(false) }
    var autoHideGeneration by remember { mutableIntStateOf(0) }
    val receiverFocusRequester = remember { FocusRequester() }
    val settingsButtonFocusRequester = remember { FocusRequester() }

    BackHandler(enabled = showSettings) { showSettings = false }

    LaunchedEffect(connected, showSettings, autoHideGeneration) {
        if (!SettingsButtonBehavior.shouldAutoHide(connected, showSettings)) {
            settingsButtonVisible = true
            if (!showSettings) settingsButtonFocusPending = true
            return@LaunchedEffect
        }

        settingsButtonVisible = true
        delay(SettingsButtonBehavior.AUTO_HIDE_DELAY_MILLIS)
        if (connected && !showSettings) {
            settingsButtonVisible = false
            receiverFocusRequester.requestFocus()
        }
    }

    LaunchedEffect(settingsButtonVisible, settingsButtonFocusPending) {
        if (settingsButtonVisible && settingsButtonFocusPending) {
            settingsButtonFocusRequester.requestFocus()
            settingsButtonFocusPending = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
            .safeDrawingPadding()
            .onPreviewKeyEvent { event ->
                val keyCode = event.nativeKeyEvent.keyCode
                if (event.nativeKeyEvent.action != android.view.KeyEvent.ACTION_DOWN) {
                    return@onPreviewKeyEvent false
                }
                if (SettingsButtonBehavior.opensSettings(keyCode)) {
                    showSettings = true
                    return@onPreviewKeyEvent true
                }
                if (connected && !showSettings && SettingsButtonBehavior.isRemoteInteraction(keyCode)) {
                    val wasHidden = !settingsButtonVisible
                    settingsButtonVisible = true
                    autoHideGeneration++
                    if (wasHidden && SettingsButtonBehavior.focusesRevealedButton(keyCode)) {
                        settingsButtonFocusPending = true
                        return@onPreviewKeyEvent true
                    }
                }
                false
            }
            .pointerInput(connected, showSettings) {
                awaitPointerEventScope {
                    while (true) {
                        awaitPointerEvent(PointerEventPass.Initial)
                        if (connected && !showSettings) {
                            settingsButtonVisible = true
                            autoHideGeneration++
                        }
                    }
                }
            }
            .focusRequester(receiverFocusRequester)
            .focusProperties { canFocus = connected && !showSettings && !settingsButtonVisible }
            .focusable(),
    ) {
        VideoViewport(
            receiver = receiver,
            videoDims = videoDims,
            fallbackWidth = selection.pixels.width,
            fallbackHeight = selection.pixels.height,
            fitMode = settings.fitMode,
            connected = connected,
            onVideoDimsChanged = {
                videoDims = it
                receiver.setDecoderResolution(it.width, it.height)
            },
            modifier = Modifier.fillMaxSize(),
        )

        if (!connected) {
            // SurfaceView owns a separate compositor surface and can retain its last buffer.
            // Keep an opaque app-layer scrim above it so a disconnected peer is never shown.
            Box(Modifier.fillMaxSize().background(Color.Black))
            Text(
                text = listenerStatus(listener),
                color = Color.White,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.align(Alignment.Center).padding(24.dp),
            )
            Text(
                text = nsdStatus(nsd),
                color = Color.LightGray,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.align(Alignment.Center).padding(top = 72.dp),
            )
        }

        peerSignal?.let { signal ->
            PeerSignalBanner(signal, Modifier.align(Alignment.TopCenter).padding(top = 24.dp))
        }

        if (connected && settings.performanceOverlay) {
            PerfHud(receiver, Modifier.align(Alignment.TopStart).padding(8.dp))
        }

        AnimatedVisibility(
            visible = settingsButtonVisible && !showSettings,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp),
        ) {
            TvActionButton(
                text = stringResource(R.string.settings),
                onClick = { showSettings = true },
                modifier = Modifier.focusRequester(settingsButtonFocusRequester),
            )
        }
    }

    if (showSettings) SettingsDialog(receiver, updateManager, activity) { showSettings = false }
}

@Composable
private fun VideoViewport(
    receiver: PhoneReceiver,
    videoDims: VideoDims?,
    fallbackWidth: Int,
    fallbackHeight: Int,
    fitMode: FitMode,
    connected: Boolean,
    onVideoDimsChanged: (VideoDims) -> Unit,
    modifier: Modifier,
) {
    Layout(
        modifier = modifier.clipToBounds(),
        content = {
            VideoSurface(
                receiver = receiver,
                videoDims = videoDims,
                connected = connected,
                onVideoDimsChanged = onVideoDimsChanged,
                modifier = Modifier.fillMaxSize(),
            )
            CursorOverlay(receiver, Modifier.fillMaxSize())
        },
    ) { measurables, constraints ->
        val sourceWidth = videoDims?.width ?: fallbackWidth
        val sourceHeight = videoDims?.height ?: fallbackHeight
        val rect = VideoTransform.destination(
            constraints.maxWidth,
            constraints.maxHeight,
            sourceWidth.coerceAtLeast(1),
            sourceHeight.coerceAtLeast(1),
            fitMode,
        )
        val childConstraints = androidx.compose.ui.unit.Constraints.fixed(
            rect.width.roundToInt().coerceAtLeast(1),
            rect.height.roundToInt().coerceAtLeast(1),
        )
        val children = measurables.map { it.measure(childConstraints) }
        layout(constraints.maxWidth, constraints.maxHeight) {
            val x = rect.left.roundToInt()
            val y = rect.top.roundToInt()
            children.forEach { it.place(x, y) }
        }
    }
}

@Composable
internal fun TvActionButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    var focused by remember { mutableStateOf(false) }
    Button(
        onClick = onClick,
        modifier = modifier
            .onFocusChanged { focused = it.isFocused }
            .border(
                width = if (focused) 3.dp else 1.dp,
                color = if (focused) Color.White else Color.Transparent,
                shape = RoundedCornerShape(14.dp),
            ),
    ) { Text(text) }
}

@Composable
private fun listenerStatus(state: ListenerState): String = when (state.phase) {
    ListenerPhase.STARTING -> stringResource(R.string.listener_starting)
    ListenerPhase.WAITING_FOR_NETWORK -> stringResource(R.string.listener_waiting_network)
    ListenerPhase.LISTENING -> stringResource(
        R.string.listener_listening,
        state.addressAndPort ?: stringResource(R.string.not_available),
    )
    ListenerPhase.RETRYING -> pluralStringResource(
        R.plurals.listener_retrying,
        state.retrySeconds,
        state.retrySeconds,
    )
    ListenerPhase.PORTS_UNAVAILABLE -> stringResource(R.string.listener_failed)
    ListenerPhase.STOPPED -> stringResource(R.string.listener_stopped)
}

@Composable
private fun nsdStatus(state: NsdState): String = when (state.phase) {
    NsdPhase.IDLE -> stringResource(R.string.nsd_idle)
    NsdPhase.REGISTERING -> stringResource(R.string.nsd_registering)
    NsdPhase.REGISTERED -> stringResource(
        R.string.nsd_registered,
        state.registeredName ?: stringResource(R.string.app_name),
    )
    NsdPhase.RETRYING -> pluralStringResource(R.plurals.nsd_retrying, state.retrySeconds, state.retrySeconds)
    NsdPhase.UNAVAILABLE -> stringResource(R.string.nsd_failed)
}

@Composable
private fun PerfHud(receiver: PhoneReceiver, modifier: Modifier = Modifier) {
    val perf by receiver.perf.collectAsState()
    Text(
        text = stringResource(
            R.string.performance_format,
            perf.fps,
            perf.megabitsPerSecond,
            perf.e2eP50Ms.toInt(),
            perf.e2eP95Ms.toInt(),
            perf.rttMs.toInt(),
            perf.approximateMemoryMb,
        ),
        color = Color.White,
        style = MaterialTheme.typography.labelSmall,
        modifier = modifier
            .background(Color.Black.copy(alpha = 0.65f), MaterialTheme.shapes.small)
            .padding(horizontal = 8.dp, vertical = 4.dp),
    )
}

@Composable
private fun PeerSignalBanner(signal: PeerSignal, modifier: Modifier = Modifier) {
    val message = when (signal) {
        PeerSignal.UpdateMac -> stringResource(R.string.peer_update_mac)
        is PeerSignal.UpdateAndroid -> signal.message ?: stringResource(R.string.peer_update_android)
        is PeerSignal.PeerReplaced -> stringResource(R.string.peer_replaced, signal.newAddress)
    }
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 16.dp),
        color = Color(0xFFB00020),
        shape = MaterialTheme.shapes.medium,
    ) {
        Text(message, color = Color.White, modifier = Modifier.padding(16.dp))
    }
}
