package io.github.mohithdas.opendisplay.tv

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.res.Configuration
import android.os.Bundle
import android.os.IBinder
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import io.github.mohithdas.opendisplay.tv.net.PhoneReceiver
import io.github.mohithdas.opendisplay.tv.service.ReceiverService
import io.github.mohithdas.opendisplay.tv.settings.AwakePolicy
import io.github.mohithdas.opendisplay.tv.settings.DisplayProfileProvider
import io.github.mohithdas.opendisplay.tv.ui.ReceiverScreen
import io.github.mohithdas.opendisplay.tv.ui.theme.OpenDisplayTheme
import io.github.mohithdas.opendisplay.tv.update.UpdateManager
import io.github.mohithdas.opendisplay.tv.util.Log

/** UI host for the foreground receiver service. Leaving with Back never stops the service. */
class MainActivity : ComponentActivity() {
    private val updateManager by lazy { UpdateManager.get(applicationContext) }
    private var boundReceiver by mutableStateOf<PhoneReceiver?>(null)
    private var bound = false
    private var receiverVisible by mutableStateOf(false)

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val receiver = (service as? ReceiverService.LocalBinder)?.receiver ?: return
            receiver.updateDisplayProfile(DisplayProfileProvider.detectActivity(this@MainActivity))
            boundReceiver = receiver
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            boundReceiver = null
            clearKeepAwake()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateManager.initialize()
        enableEdgeToEdge()
        val serviceIntent = Intent(this, ReceiverService::class.java)
        startForegroundService(serviceIntent)
        bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE)
        bound = true

        setContent {
            OpenDisplayTheme {
                val receiver = boundReceiver
                if (receiver == null) {
                    Box(Modifier.fillMaxSize().background(Color.Black)) {
                        Text(
                            stringResource(R.string.starting),
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.align(Alignment.Center),
                        )
                    }
                } else {
                    val settings by receiver.settings.collectAsState()
                    val connected by receiver.connected.collectAsState()
                    LaunchedEffect(settings.keepAwakePolicy, connected, receiverVisible) {
                        val keepAwake = AwakePolicy.shouldKeepScreenAwake(
                            settings.keepAwakePolicy,
                            receiverVisible,
                            connected,
                        )
                        if (keepAwake) {
                            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                        } else {
                            clearKeepAwake()
                        }
                    }
                    ReceiverScreen(receiver, updateManager)
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        receiverVisible = true
    }

    override fun onStop() {
        receiverVisible = false
        clearKeepAwake()
        super.onStop()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        Log.info("configuration changed; recalculating display profile")
        boundReceiver?.updateDisplayProfile(DisplayProfileProvider.detectActivity(this))
    }

    override fun onDestroy() {
        clearKeepAwake()
        if (bound) {
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }

    private fun clearKeepAwake() {
        window.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
    }
}
